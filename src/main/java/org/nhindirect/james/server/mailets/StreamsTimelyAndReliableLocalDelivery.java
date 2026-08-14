package org.nhindirect.james.server.mailets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamsTimelyAndReliableLocalDelivery extends LocalDelivery//TimelyAndReliableLocalDelivery
{
	
	private static final Logger log = LoggerFactory.getLogger(StreamsTimelyAndReliableLocalDelivery.class);
	
	protected static final String DELAY_NOTIFICATION_HEADER = "X-Delay-Dispatched-MDN";
	
	protected static final String SUPRESS_NOTIFICATIONS_ADDRESSES = "SuppressNotificationsForAddresses";

	protected static final String DISPATCHED_MDN_DELAY = "DispatchedMDNDelay";

	protected static final String DELAYED_DISPATCH_MDN_ADDRESSES = "DelayedDispatchMDNAddresses";

	protected static StreamsTimelyAndReliableLocalDelivery mailetContextInstance;

	protected NotificationProducer notificationProducer;

	protected UsersRepository uRepo;

	protected List<String> suppressNotificationAddresses;

	protected List<String> delayedDispatchMDNAddresses;

	protected int dispatchedMDNDelay;

	/*
	 * Shared scheduler used to release delayed MDN "dispatched" notifications at a later time without
	 * blocking the calling thread. Multiple messages may be pending release at any given time, each with
	 * its own delay, which a ScheduledExecutorService handles natively.
	 */
	protected static final ScheduledExecutorService dispatchedMDNScheduler = Executors.newSingleThreadScheduledExecutor(r ->
	{
		final Thread t = new Thread(r, "dispatched-mdn-sender");
		t.setDaemon(true);
		return t;
	});

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
    public void init() throws MessagingException {
    	
		super.init();
		
        String supressionAddresses = getInitParameter(SUPRESS_NOTIFICATIONS_ADDRESSES, "");

        suppressNotificationAddresses  = StringUtils.hasText(supressionAddresses) ?
        		List.of(supressionAddresses.split(",")) : List.of();

        String delayedDispatchAddresses = getInitParameter(DELAYED_DISPATCH_MDN_ADDRESSES, "");

        delayedDispatchMDNAddresses = StringUtils.hasText(delayedDispatchAddresses) ?
        		List.of(delayedDispatchAddresses.split(",")) : List.of();

        String sDispatchedDelay = getInitParameter(DISPATCHED_MDN_DELAY, "0");
        
		try
		{
			dispatchedMDNDelay = Integer.valueOf(sDispatchedDelay).intValue();
		}
		catch (NumberFormatException e)
		{
			// in case of parsing exceptions
			dispatchedMDNDelay = 0;
		}
		
		log.info("Suppression config for StreamsTimelyAndReliableLocalDelivery: \r\n\tsuppressNotificationAddresses: {}"
				+ " \r\n\tdelayedDispatchMDNAddresses: {} \r\n\tdispatchedMDNDelay: {}", supressionAddresses, delayedDispatchAddresses, dispatchedMDNDelay);
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
					// Normalize plus-addressed recipients (RFC 5233) for repository lookup and delivery
					MailAddress normalizedRecip = recip;
					String localPart = recip.getLocalPart();
					if (localPart.contains("+")) {
						String normalizedLocal = localPart.substring(0, localPart.indexOf("+"));
						normalizedRecip = new MailAddress(normalizedLocal + "@" + recip.getDomain().asString());
					}
					Username uName = uRepo.getUsername(normalizedRecip);
					if (uRepo.contains(uName)) {
						foundRecipsMailAddr.add(normalizedRecip);
						if (recip.toInternetAddress().isPresent())
							foundRecips.add(recip.toInternetAddress().get());
					}

					else
						if (recip.toInternetAddress().isPresent())
							unknownRecips.add(recip.toInternetAddress().get());
				} catch (UsersRepositoryException | AddressException e) {
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

				// send back an MDN dispatched message for the found recipients, excluding any recipient
				// configured to have dispatched notifications suppressed
				final List<InternetAddress> notificationRecips = foundRecips.stream()
						.filter(recip -> !MailUtils.matchesAddress(recip, suppressNotificationAddresses))
						.collect(Collectors.toList());

				final Collection<NotificationMessage> notifications = notificationRecips.isEmpty() ? List.of()
						: notificationProducer.produce(new Message(msg), notificationRecips);
				if (notifications != null && notifications.size() > 0)
				{
					log.debug("Sending MDN \"dispatched\" messages");
					// create a message for each notification and put it on James "stack"
					for (NotificationMessage message : notifications)
					{
						try
						{
							message.saveChanges();

							// the notification's From header carries the recipient it was generated for
							if (MailUtils.matchesFromAddress(message, delayedDispatchMDNAddresses)) {
								int resolvedDelay = resolveDispatchedMDNDelay(msg);
								
								if (resolvedDelay > 0)
									scheduleDelayedMDNDispatch(message, resolvedDelay);
							}
								
							else
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
				MailUtils.sendDSN(txToTrack, unknownRecips, false, suppressNotificationAddresses);
			}
				
		}
		else
		{
			log.warn("Message delivery failed.  Generating bounce messages for message id {}", mail.getMessage().getMessageID());
			// create a DSN message regardless if timely and reliable was requested for all recipients
			if (txToTrack != null && txToTrack.getMsgType() == TxMessageType.IMF)
				MailUtils.sendDSN(txToTrack, recipients, false, suppressNotificationAddresses);
		}
		
		log.debug("Exiting timely and reliable service method.");
	}

	/*
	 * Determines the delay, in milliseconds, to use for a delayed MDN "dispatched" notification.
	 * If the originating message carries a valid DELAY_NOTIFICATION_HEADER value (in minutes), that
	 * value takes precedence; otherwise the configured dispatchedMDNDelay (already in milliseconds) is used.
	 */
	protected int resolveDispatchedMDNDelay(MimeMessage msg) throws MessagingException
	{
		final String headerValue = msg.getHeader(DELAY_NOTIFICATION_HEADER, null);

		if (StringUtils.hasText(headerValue))
		{
			try
			{
				// value of header is in minutes, so adjust to milliseconds
				return Integer.parseInt(headerValue.trim()) * 60 * 1000;
			}
			catch (NumberFormatException e)
			{
				log.warn("Invalid value \"{}\" for header {}.  Falling back to configured dispatchedMDNDelay.",
						headerValue, DELAY_NOTIFICATION_HEADER);
			}
		}

		return dispatchedMDNDelay;
	}

	/*
	 * Releases an MDN "dispatched" notification onto the stream after the given delay (in milliseconds)
	 * has elapsed, without blocking the calling thread.
	 */
	protected void scheduleDelayedMDNDispatch(NotificationMessage message, int delay)
	{
		log.debug("Delaying dispatch of MDN message by {} ms", delay);

		dispatchedMDNScheduler.schedule(() ->
		{
			try
			{
				MailUtils.sendMessageToStream(message);
			}
			catch (Throwable t)
			{
				// don't kill the process if this fails
				log.error("Error sending delayed MDN dispatched message.", t);
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

}
