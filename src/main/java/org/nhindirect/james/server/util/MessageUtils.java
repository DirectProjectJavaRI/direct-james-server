package org.nhindirect.james.server.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Common methods used for inspecting and gathering information about messages
 * @author Greg Meyer
 * @Since 6.0
 */
@Slf4j
public class MessageUtils
{
	/**
	 * Gets the sender of the message.
	 * @param mail The mail object to get the mail information from.
	 * @return The sender of the message.
	 * @throws MessagingException
	 */
	public static InternetAddress getMailSender(SMTPMailMessage mail) throws MessagingException
	{
		// get the sender
		final InternetAddress senderAddr = getSender(mail);
		if (senderAddr == null)
			throw new MessagingException("Failed to process message.  The sender cannot be null or empty.");
						
			// not the best way to do this
		return senderAddr;
	}
	
	/**
	 * Gets the sender attribute of a Mail message
	 * @param mail The message to retrive the sender from
	 * @return The message sender.
	 */
	public static InternetAddress getSender(SMTPMailMessage mail) 
	{
		InternetAddress retVal = null;
		
		if (mail.getMailFrom() != null)
			retVal = mail.getMailFrom();	
		else
		{
			// try to get the sender from the message
			Address[] senderAddr = null;
			try
			{
				if (mail.getMimeMessage() == null)
					return null;
				
				senderAddr = mail.getMimeMessage().getFrom();
				if (senderAddr == null || senderAddr.length == 0)
					return null;
			}
			catch (MessagingException e)
			{
				return null;
			}
						
			// not the best way to do this
			retVal = (InternetAddress)senderAddr[0];	
		}
	
		return retVal;
	}	
	
	/**
	 * Get the recipients of a message by retrieving the recipient list from the SMTP envelope first, then falling back to the recipients
	 * in the message if the recipients cannot be retrieved from the SMTP envelope.
	 * @param mail The mail object that contains information from the SMTP envelope.
	 * @return Collection of message recipients.
	 * @throws MessagingException
	 */
	public static List<InternetAddress> getMailRecipients(SMTPMailMessage mail) throws MessagingException
	{
		final List<InternetAddress> recipients = new ArrayList<>();		
		
		// uses the RCPT TO commands
		final Collection<InternetAddress> recips = mail.getRecipientAddresses();
		if (recips == null || recips.size() == 0)
		{
			// fall back to the mime message list of recipients
			final Address[] recipsAddr = mail.getMimeMessage().getAllRecipients();
			for (Address addr : recipsAddr)
			{
				recipients.add(new InternetAddress(addr.toString()));
			}
		}
		else
			for (InternetAddress addr : recips)
				recipients.add(addr);

		return recipients;
	}	
	
	/**
	 * Creates a trackable monitoring object for a message. 
	 * @param msg The message that is being processed
	 * @param sender The sender of the message
	 * @param recipients The message recipients
	 * @param txParser Parser to extract Tx details from the memssage.
	 * @return A trackable Tx object.
	 */
	public static Tx getTxToTrack(MimeMessage msg, InternetAddress sender, List<InternetAddress> recipients, TxDetailParser txParser)
	{		
		if (txParser == null)
			return null;
				
		try
		{	
			
			final Map<String, TxDetail> details = txParser.getMessageDetails(msg);
			
			if (sender != null)
				details.put(TxDetailType.FROM.getType(), new TxDetail(TxDetailType.FROM, sender.getAddress().toLowerCase(Locale.getDefault())));
			if (recipients != null && !recipients.isEmpty())
				details.put(TxDetailType.RECIPIENTS.getType(), new TxDetail(TxDetailType.RECIPIENTS, recipients.toString().toLowerCase(Locale.getDefault())));
			
			
			return new Tx(TxUtil.getMessageType(msg), details);
		}
		///CLOVER:OFF
		catch (Exception e)
		{
			log.warn("Failed to parse message to Tx object.", e);
			return null;
		}
		///CLOVER:ON
	}	
	
	/**
	 * 
	 * Determine if the recipient has been rejected
	 * 
	 * @param rctpAdd
	 * @param rejectedRecips
	 * @return
	 */
	public static boolean isRcptRejected(InternetAddress rctpAdd, List<InternetAddress> rejectedRecips)
	{
		for (InternetAddress rejectedRecip : rejectedRecips)
			if (rejectedRecip.getAddress().equals(rctpAdd.toString()))
				return true;
		
		return false;
	}	
	
}