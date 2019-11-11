package org.nhindirect.james.server.mailets;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.smtp.NotificationProducer;
import org.nhindirect.gateway.smtp.NotificationSettings;
import org.nhindirect.gateway.smtp.ReliableDispatchedNotificationProducer;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamsTimelyAndReliableLocalDelivery extends LocalDelivery//TimelyAndReliableLocalDelivery
{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamsTimelyAndReliableLocalDelivery.class);
	
	protected static final String DISPATCHED_MDN_DELAY = "DispatchedMDNDelay";
	
	protected static StreamsTimelyAndReliableLocalDelivery mailetContextInstance;
	
	protected NotificationProducer notificationProducer;
	
	@Inject
	public StreamsTimelyAndReliableLocalDelivery(UsersRepository usersRepository, @Named("mailboxmanager") MailboxManager mailboxManager,
			MetricFactory metricFactory)
	{
		super(usersRepository, mailboxManager, metricFactory);
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
		LOGGER.debug("Calling timely and reliable service method.");
		
		boolean deliverySuccessful = false;
		
		final MimeMessage msg = mail.getMessage();
		final boolean isReliableAndTimely = TxUtil.isReliableAndTimelyRequested(msg);
		
		final SMTPMailMessage smtpMailMessage = MailUtils.mailToSMTPMailMessage(mail);
		
		final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMailMessage);
		
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMailMessage);
		
		
		try
		{
			super.service(mail);
			deliverySuccessful = true;
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to deliver mail locally.", e);
		}
		
		final Tx txToTrack = MailUtils.getTxToTrack(msg, sender, recipients);
		
		if (deliverySuccessful)
		{	
			if (isReliableAndTimely && txToTrack.getMsgType() == TxMessageType.IMF)
			{

				// send back an MDN dispatched message
				final Collection<NotificationMessage> notifications = 
						notificationProducer.produce(new Message(msg), recipients.toInternetAddressCollection());
				if (notifications != null && notifications.size() > 0)
				{
					LOGGER.debug("Sending MDN \"dispatched\" messages");
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
							LOGGER.error("Error sending MDN dispatched message.", t);
						}
						///CLOVER:ON
					}
				}
			}
		}
		else
		{
			// create a DSN message regarless if timely and reliable was requested
			if (txToTrack != null && txToTrack.getMsgType() == TxMessageType.IMF)
				MailUtils.sendDSN(txToTrack, recipients, false);
		}
		
		LOGGER.debug("Exiting timely and reliable service method.");
	}	

}
