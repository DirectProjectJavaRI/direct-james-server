package org.nhindirect.james.server;

import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nhind.config.rest.DomainService;
import org.nhindirect.james.server.streams.sinks.STALastMileDeliverySink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource("classpath:testapp.properties")
public abstract class SpringBaseTest
{
	@Autowired
	protected RestTemplate restTemplate;
	
	@Autowired
	protected DomainService domService;
	
	@Autowired
	protected STALastMileDeliverySink deliverySink;
	
	@Value("${james.server.test.user}")
	protected String testUser;
	
	@Value("${james.server.test.password}")
	protected String testPass;
	
	@BeforeEach
	public void addUsers() throws Exception
	{
		/*
		 * Add a user if not already added
		 */
		final String userPassword = "{\"password\":\"" + testPass + "\"}";
		
		restTemplate.put("http://localhost:8080/users/{username}", userPassword, testUser);
		
		/*
		 * Delete any messages that may exist if the user already exists
		 */
		
	    Properties properties = System.getProperties();
	    Session session = Session.getDefaultInstance(properties);
	    
	    Store store = session.getStore("pop3");
	    store.connect("localhost", 1110, testUser, testPass);
	    
	    Folder inbox = store.getFolder("Inbox");
	    inbox.open(Folder.READ_WRITE);
	    
	    Message[] messages = inbox.getMessages();
	    
	    if (messages != null)
	    {
	    	for (Message msg : messages)
	    	{
	    		msg.setFlag(Flags.Flag.DELETED, true);
	    	}
	    }
	    
	    inbox.close(true);
	    store.close();
	    
	}
	
	
}
