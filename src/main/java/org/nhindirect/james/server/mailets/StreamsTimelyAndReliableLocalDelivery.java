package org.nhindirect.james.server.mailets;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mailet;
//import org.nhindirect.gateway.smtp.james.mailet.TimelyAndReliableLocalDelivery;

public class StreamsTimelyAndReliableLocalDelivery extends LocalDelivery//TimelyAndReliableLocalDelivery
{
	protected static StreamsTimelyAndReliableLocalDelivery mailetContextInstance;
	
	@Inject
	public StreamsTimelyAndReliableLocalDelivery(UsersRepository usersRepository, MailboxManager mailboxManager,
			MetricFactory metricFactory)
	{
		super(usersRepository, mailboxManager, metricFactory);
         /*
		 * Set the static reference used by the Spring Cloud streams processor (i.e. the processLastMileMessage() method).
		 * Once this instance has been set, then notify any thread that is blocked.
		 */
		synchronized(StreamsTimelyAndReliableLocalDelivery.class)
		{
			if (usersRepository != null)
			{
				mailetContextInstance = this;
				StreamsTimelyAndReliableLocalDelivery.class.notifyAll();
			}
		}
	}
	
	public static Mailet getStaticMailet()
	{
		synchronized(StreamsTimelyAndReliableLocalDelivery.class)
		{
			return mailetContextInstance;
		}
	}

}
