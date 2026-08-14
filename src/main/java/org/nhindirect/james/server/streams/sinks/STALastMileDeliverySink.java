package org.nhindirect.james.server.streams.sinks;

import java.util.List;
import java.util.function.Consumer;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.nhindirect.common.javaxcompat.mail.SMTPMailMessage;
import org.nhindirect.common.javaxcompat.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.james.server.mailets.MailUtils;
import org.nhindirect.james.server.mailets.StreamsTimelyAndReliableLocalDelivery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.github.fge.lambdas.Throwing;


import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class STALastMileDeliverySink
{	
	public STALastMileDeliverySink()
	{

	}
	
	@Bean
	public Consumer<Message<?>> directStaLastMileInput()
	{
		return streamMsg -> 
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
			
			final List<MailAddress> recips = smtpMessage.getRecipientAddresses().stream()
	        	.map(Throwing.function(MailUtils::castToMailAddress).sneakyThrow())
	            .toList();
			
			try
			{
				final MailImpl mail = MailImpl.builder().name("DirectMailBuilder")
						.sender(new MailAddress(smtpMessage.getMailFrom()))
						.addRecipients(recips)
						.mimeMessage(smtpMessage.getMimeMessage()).build();
				
				try {
					log.info("Processing last mile delivery for from {} to {} with message id {}", smtpMessage.getMailFrom().toString(), 
							toRecipsPrettingString(recips), smtpMessage.getMimeMessage().getMessageID());
					
					StreamsTimelyAndReliableLocalDelivery.getStaticMailet().service(mail);
				}
				finally {
					mail.dispose();
				}
			}
			catch (MessagingException e)
			{
				throw new RuntimeException(e);
			}
		};
	}
	
	protected String toRecipsPrettingString(List<MailAddress> recips)
	{
		final String[] addrs = new String[recips.size()];
		
		int idx = 0;
		for (MailAddress addr : recips)
			addrs[idx++] = addr.asPrettyString();
		
		return String.join(",", addrs);
	}
}
