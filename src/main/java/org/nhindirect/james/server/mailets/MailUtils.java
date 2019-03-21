package org.nhindirect.james.server.mailets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.james.server.spring.DSNCreatorFactory;
import org.nhindirect.james.server.streams.SmtpGatewayMessageSource;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class MailUtils
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MailUtils.class);	
	
	protected static TxDetailParser txParser = new DefaultTxDetailParser();
	
	/**
	 * Converts an Apache James Mail message to the common SMTPMailMessage object;
	 * @param mail The Apache James smtp message
	 * @return An SMTPMailMessage message instance container information from the Apache James mail object;
	 */
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
	
	/**
	 * Creates a trackable monitoring object for a message. 
	 * @param msg The message that is being processed
	 * @param sender The sender of the message
	 * @return A trackable Tx object.
	 */
	protected static Tx getTxToTrack(MimeMessage msg, NHINDAddress sender, NHINDAddressCollection recipients)
	{		
		return MessageUtils.getTxToTrack(msg, sender, recipients, txParser);
	}
	
	protected static void sendDSN(Tx tx, NHINDAddressCollection undeliveredRecipeints, boolean useSenderAsPostmaster)
	{
		try
		{
			DSNCreator dsnCreator = DSNCreatorFactory.getFailedDeliverDSNCreator();
			if (dsnCreator != null)
			{
				final Collection<MimeMessage> msgs = dsnCreator.createDSNFailure(tx, undeliveredRecipeints, useSenderAsPostmaster);
				if (msgs != null && msgs.size() > 0)
					for (MimeMessage msg : msgs)
						sendMessageToStream(msg);
			}
		}
		catch (Throwable e)
		{
			// don't kill the process if this fails
			LOGGER.error("Error sending DSN failure message.", e);
		}
	}	
	
	protected static void sendMessageToStream(MimeMessage msg) throws Exception
	{
		final ImmutableList<MailAddress> recips = Arrays.asList(msg.getAllRecipients()).stream()
	        	.map(Throwing.function(MailUtils::castToMailAddress).sneakyThrow())
	        	.collect(Guavate.toImmutableList());
			
			final Mail mail = MailImpl.builder()
					.sender(castToMailAddress(msg.getFrom()[0]))
					.recipients(recips)
					.mimeMessage(msg).build();
			
			final SmtpGatewayMessageSource messageSource = SmtpGatewayMessageSource.getMessageSourceInstance();
			if (messageSource != null)
			{
				messageSource.forwardSMTPMessage(mailToSMTPMailMessage(mail));
			}
	}
	
    public static MailAddress castToMailAddress(Address address) throws AddressException 
    {
        Preconditions.checkArgument(address instanceof InternetAddress);
        return new MailAddress((InternetAddress) address);
    }
}
