package org.nhindirect.james.server.mailets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.james.server.streams.SmtpGatewayMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToStaAgentStream extends GenericMailet
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(ToStaAgentStream.class);
	
	public ToStaAgentStream()
	{
		
	}

	@Override
	public void service(Mail mail) throws MessagingException
	{
		LOGGER.info("Receiving message to deliver to STA.  Message id: {}", mail.getMessage().getMessageID());
		
		final SMTPMailMessage smtpMailMessage = mailToSMTPMailMessage(mail);
		
		final SmtpGatewayMessageSource messageSource = SmtpGatewayMessageSource.getMessageSourceInstance();
		if (messageSource != null)
		{
			messageSource.forwardSMTPMessage(smtpMailMessage);
			mail.setState(Mail.GHOST);
			
			LOGGER.info("Message sent to STA.  Message id: {}", mail.getMessage().getMessageID());
			
			return;
		}
		
		LOGGER.warn("Message STA source is not available to process message id: {}", mail.getMessage().getMessageID());
	}
	
	/**
	 * Converts an Apache James Mail message to the common SMTPMailMessage object;
	 * @param mail The Apache James smtp message
	 * @return An SMTPMailMessage message instance container information from the Apache James mail object;
	 */
	@SuppressWarnings("deprecation")
	public static SMTPMailMessage mailToSMTPMailMessage(Mail mail) throws MessagingException
	{
		if (mail == null)
			return null;
		
		List<InternetAddress> toAddrs = new ArrayList<>();
		final InternetAddress fromAddr = (mail.getSender() == null) ? null : mail.getSender().toInternetAddress();
		// uses the RCPT TO commands
		final Collection<MailAddress> recips = mail.getRecipients();
		if (recips == null || recips.size() == 0)
		{
			// fall back to the mime message list of recipients
			final Address[] recipsAddr = mail.getMessage().getAllRecipients();
			for (Address addr : recipsAddr)
				toAddrs.add((InternetAddress)addr);
		}
		else
		{
			toAddrs = recips.stream().
					map(toAddr -> toAddr.toInternetAddress()).collect(Collectors.toList());

		}
		
		return new SMTPMailMessage(mail.getMessage(), toAddrs, fromAddr);
	}
}
