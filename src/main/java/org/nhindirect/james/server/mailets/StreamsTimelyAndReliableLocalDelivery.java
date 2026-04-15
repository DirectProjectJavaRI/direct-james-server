package org.nhindirect.james.server.mailets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.nhindirect.common.javaxcompat.mail.SMTPMailMessage;
import org.nhindirect.common.javaxcompat.tx.TxUtil;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.javaxcompat.smtp.NotificationProducer;
import org.nhindirect.gateway.javaxcompat.smtp.NotificationSettings;
import org.nhindirect.gateway.javaxcompat.smtp.ReliableDispatchedNotificationProducer;
import org.nhindirect.gateway.javaxcompat.util.MessageUtils;
import org.nhindirect.stagent.javaxcompat.mail.Message;
import org.nhindirect.stagent.javaxcompat.mail.notifications.NotificationMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamsTimelyAndReliableLocalDelivery extends LocalDelivery//TimelyAndReliableLocalDelivery
{
	
	protected static final String DISPATCHED_MDN_DELAY = "DispatchedMDNDelay";
	
	protected static StreamsTimelyAndReliableLocalDelivery mailetContextInstance;
	
	protected NotificationProducer notificationProducer;
	
	protected UsersRepository uRepo;
	
	@Inject
	public StreamsTimelyAndReliableLocalDelivery(UsersRepository usersRepository, @Named("mailboxmanager") MailboxManager mailboxManager,
			MetricFactory metricFactory)
	{
		super(usersRepository, mailboxManager, metricFactory);
		uRepo = usersRepository;
         /*
		 * Set the static reference used by the Spring Cloud streams processor (i.e. the processLastMileMessage() method).
		 * Once this instance has been set, then notify any thread that is blocked.
		 */
		synchronized(StreamsTimelyAndReliableLocalDelivery.class)
		{
			if (usersRepository != null)
			{
				mailetContextInstance = this;
				StreamsTimelyAndReliableLocalDelivery.class.notifyAll();
			}
		}
		
		notificationProducer = new ReliableDispatchedNotificationProducer(new NotificationSettings(true, "Local Direct Delivery Agent", "Your message was successfully dispatched."));
	}
	
	public static Mailet getStaticMailet()
	{
		synchronized(StreamsTimelyAndReliableLocalDelivery.class)
		{
			return mailetContextInstance;
		}
	}
	
	@Override
	public void service(Mail mail) throws MessagingException 
	{
		log.debug("Calling timely and reliable service method.");
		
		boolean deliverySuccessful = false;
		
		final MimeMessage msg = mail.getMessage();
		final boolean isReliableAndTimely = TxUtil.isReliableAndTimelyRequested(msg);
		
		final SMTPMailMessage smtpMailMessage = MailUtils.mailToSMTPMailMessage(mail);
		
		final List<InternetAddress> recipients = MessageUtils.getMailRecipients(smtpMailMessage);
		
		final InternetAddress sender = MessageUtils.getMailSender(smtpMailMessage);
		
		
		final List<InternetAddress> foundRecips = new ArrayList<>();
		final List<MailAddress> foundRecipsMailAddr = new ArrayList<>();
		final List<InternetAddress> unknownRecips = new ArrayList<>();
		try
		{

			mail.getRecipients().stream().forEach(recip -> {
				try {
					Username uName = uRepo.getUsername(recip);
					if (uRepo.contains(uName)) {
						foundRecipsMailAddr.add(recip);
						if (recip.toInternetAddress().isPresent())
							foundRecips.add(recip.toInternetAddress().get()); 
					}
				
					else
						if (recip.toInternetAddress().isPresent())
							unknownRecips.add(recip.toInternetAddress().get());
				} catch (UsersRepositoryException e) {
					if (recip.toInternetAddress().isPresent())
						unknownRecips.add(recip.toInternetAddress().get());
				}
			});
			
			if (!foundRecips.isEmpty()) {
				mail.setRecipients(foundRecipsMailAddr);
				super.service(mail);
				deliverySuccessful = true;
			}
			else
				log.warn("No configured recipients accounts were found for message id, so delivery will fail.  Will bounce the message",
						mail.getMessage().getMessageID());

		}
		catch (Exception e)
		{
			log.error("Failed to deliver mail locally.", e);
		}
		
		final Tx txToTrack = MailUtils.getTxToTrack(msg, sender, recipients);
		
		if (deliverySuccessful)
		{	
			if (isReliableAndTimely && txToTrack.getMsgType() == TxMessageType.IMF)
			{

				// send back an MDN dispatched message for the found recipients
				final Collection<NotificationMessage> notifications = 
						notificationProducer.produce(new Message(msg), foundRecips);
				if (notifications != null && notifications.size() > 0)
				{
					log.debug("Sending MDN \"dispatched\" messages");
					// create a message for each notification and put it on James "stack"
					for (NotificationMessage message : notifications)
					{
						try
						{
							message.saveChanges();
							
							MailUtils.sendMessageToStream(message);
						}
						///CLOVER:OFF
						catch (Throwable t)
						{
							// don't kill the process if this fails
							log.error("Error sending MDN dispatched message.", t);
						}
						///CLOVER:ON
					}
				}
			}
			
			// if there are any unknown recipients, then we need to bounce those
			if (!unknownRecips.isEmpty() && txToTrack != null && txToTrack.getMsgType() == TxMessageType.IMF)
			{
				log.warn("Found unknown recipients.  Generating bounce messages for message id {} and recipients {}", 
						mail.getMessage().getMessageID(), unknownRecips);
				MailUtils.sendDSN(txToTrack, unknownRecips, false);
			}
				
		}
		else
		{
			log.warn("Message delivery failed.  Generating bounce messages for message id {}", mail.getMessage().getMessageID());
			// create a DSN message regardless if timely and reliable was requested for all recipients
			if (txToTrack != null && txToTrack.getMsgType() == TxMessageType.IMF)
				MailUtils.sendDSN(txToTrack, recipients, false);
		}
		
		log.debug("Exiting timely and reliable service method.");
	}	

}
