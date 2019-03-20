package org.nhindirect.james.server.streams.sinks;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.james.core.MailAddress;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.james.server.mailets.StreamsTimelyAndReliableLocalDelivery;
import org.nhindirect.james.server.streams.STALastMileDeliveryInput;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@EnableBinding(STALastMileDeliveryInput.class)
public class STALastMileDeliverySink
{	
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
        	.map(Throwing.function(STALastMileDeliverySink::castToMailAddress).sneakyThrow())
        	.collect(Guavate.toImmutableList());
		
		final Mail mail = MailImpl.builder()
				.sender(new MailAddress(smtpMessage.getMailFrom()))
				.recipients(recips)
				.mimeMessage(smtpMessage.getMimeMessage()).build();
		
		StreamsTimelyAndReliableLocalDelivery.getStaticMailet().service(mail);
	}
	
    protected static MailAddress castToMailAddress(Address address) throws AddressException 
    {
        Preconditions.checkArgument(address instanceof InternetAddress);
        return new MailAddress((InternetAddress) address);
    }
}
