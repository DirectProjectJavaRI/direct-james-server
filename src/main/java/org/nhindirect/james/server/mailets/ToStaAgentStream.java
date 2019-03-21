package org.nhindirect.james.server.mailets;

import javax.mail.MessagingException;

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
		
		final SMTPMailMessage smtpMailMessage = MailUtils.mailToSMTPMailMessage(mail);
		
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
}
