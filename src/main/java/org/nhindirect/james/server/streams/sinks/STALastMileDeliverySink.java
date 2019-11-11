package org.nhindirect.james.server.streams.sinks;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.james.server.mailets.MailUtils;
import org.nhindirect.james.server.mailets.StreamsTimelyAndReliableLocalDelivery;
import org.nhindirect.james.server.streams.STALastMileDeliveryInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

@EnableBinding(STALastMileDeliveryInput.class)
public class STALastMileDeliverySink
{	
	private static final Logger LOGGER = LoggerFactory.getLogger(STALastMileDeliverySink.class);
	
	public STALastMileDeliverySink()
	{

	}
	
	@StreamListener(target = STALastMileDeliveryInput.STA_LAST_MILE_INPUT)
	public void processLastMileMessage(Message<?> streamMsg) throws MessagingException
	{
		/*
		 * This blocks the processing of messages until the mailet instance has been created
		 */
		synchronized(StreamsTimelyAndReliableLocalDelivery.class)
		{
			if (StreamsTimelyAndReliableLocalDelivery.getStaticMailet() == null)
			{
				try
				{
					STALastMileDeliverySink.class.wait();
				}
				catch (InterruptedException e) {/* no-op */}
			}
		}
		
		/*
		 * Create the MAIL message and send it on to the mailet.
		 */
		final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
		
		final ImmutableList<MailAddress> recips = smtpMessage.getRecipientAddresses().stream()
        	.map(Throwing.function(MailUtils::castToMailAddress).sneakyThrow())
        	.collect(Guavate.toImmutableList());
		
		final Mail mail = MailImpl.builder()
				.sender(new MailAddress(smtpMessage.getMailFrom()))
				.recipients(recips)
				.mimeMessage(smtpMessage.getMimeMessage()).build();
		
		LOGGER.info("Processing last mile delivery for from {} to {} with message id {}", smtpMessage.getMailFrom().toString(), 
				toRecipsPrettingString(recips), smtpMessage.getMimeMessage().getMessageID());
		
		StreamsTimelyAndReliableLocalDelivery.getStaticMailet().service(mail);
	}
	
	protected String toRecipsPrettingString(ImmutableList<MailAddress> recips)
	{
		final String[] addrs = new String[recips.size()];
		
		int idx = 0;
		for (MailAddress addr : recips)
			addrs[idx++] = addr.asPrettyString();
		
		return String.join(",", addrs);
	}
}
