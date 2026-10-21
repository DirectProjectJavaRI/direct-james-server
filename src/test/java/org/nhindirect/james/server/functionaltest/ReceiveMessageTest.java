package org.nhindirect.james.server.functionaltest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.james.server.SpringBaseTest;

@Disabled
public class ReceiveMessageTest extends SpringBaseTest
{	
	
	protected void createAndDeliverMessage(String from, String to) throws Exception
	{
		final MimeMessage testMessage = new MimeMessage((Session)null);
		testMessage.setFrom(new InternetAddress(from));
		testMessage.setRecipient(RecipientType.TO, new InternetAddress(to));
		testMessage.setText("Test Message");
		testMessage.saveChanges();
		
		final List<InternetAddress> recipients = Arrays.asList((InternetAddress[])testMessage.getAllRecipients());
		final SMTPMailMessage mailMessage = new SMTPMailMessage(testMessage, recipients, (InternetAddress)testMessage.getFrom()[0]);
		deliverySink.directStaLastMileInput().accept(SMTPMailMessageConverter.toStreamMessage(mailMessage));
	}
	
	protected Folder getInbox() throws Exception
	{
	    final Properties properties = System.getProperties();
	    final Session session = Session.getDefaultInstance(properties);
	    
	    Store store = session.getStore("pop3");
	    store.connect("localhost", 1110, testUser, testPass);
	    
	    Folder inbox = store.getFolder("Inbox");
	    inbox.open(Folder.READ_ONLY);
	    
	    return inbox;
	}
	
	@Test
	public void testReceiveExternalMessageToValidAccount_assertSingleMessage() throws Exception
	{
		createAndDeliverMessage("incoming@externaldomain.com", testUser);
		
		final Folder inbox = getInbox();
	    
	    Message[] messages = inbox.getMessages();
	    
	    try
	    {
	    	assertEquals(1, messages.length);
	    }
	    finally
	    {
	    	inbox.close(true);
	    }
	    
	}
	
	@Test
	public void testReceiveExternalMessageToInValidAccount_assertNoMessages() throws Exception
	{		
		createAndDeliverMessage("incoming@externaldomain.com", "testtttt@testdomain.com");
		
		final Folder inbox = getInbox();
	    
	    Message[] messages = inbox.getMessages();
	    
	    try
	    {
	    	assertEquals(0, messages.length);
	    }
	    finally
	    {
	    	inbox.close(true);
	    }
	}
}
