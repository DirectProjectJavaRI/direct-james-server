package org.nhindirect.james.server.spring;

import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.FailedDeliveryDSNCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DSNCreatorFactory
{
	protected static DSNCreator dsnCreator;
	
	@Bean
	@ConditionalOnMissingBean
	public DSNCreator failedDeliveryDSNCreator()
	{
		dsnCreator = new FailedDeliveryDSNCreator(null);
		
		return dsnCreator;
	}	
	
	public static DSNCreator getFailedDeliverDSNCreator()
	{
		return dsnCreator;
	}
}
