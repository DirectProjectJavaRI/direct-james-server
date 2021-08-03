package org.nhindirect.james.server.mailets;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.james.server.streams.SmtpGatewayMessageSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToStaAgentStream extends GenericMailet
{
	public ToStaAgentStream()
	{
		
	}

	@Override
	public void service(Mail mail) throws MessagingException
	{
		log.info("Receiving message to deliver to STA.  Message id: {}", mail.getMessage().getMessageID());
		
		final SMTPMailMessage smtpMailMessage = MailUtils.mailToSMTPMailMessage(mail);
		
		final SmtpGatewayMessageSource messageSource = SmtpGatewayMessageSource.getMessageSourceInstance();
		if (messageSource != null)
		{
			messageSource.forwardSMTPMessage(smtpMailMessage);
			mail.setState(Mail.GHOST);
			
			log.info("Message sent to STA.  Message id: {}", mail.getMessage().getMessageID());
			
			return;
		}
		
		log.warn("Message STA source is not available to process message id: {}", mail.getMessage().getMessageID());
	}
}
