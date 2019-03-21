package org.nhindirect.james.server.spring;

import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.feign.AddressClient;
import org.nhind.config.rest.feign.DomainClient;
import org.nhind.config.rest.impl.DefaultAddressService;
import org.nhind.config.rest.impl.DefaultDomainService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients({"org.nhind.config.rest.feign"})
public class ConfigServiceClientConfig
{		
	
	@Bean
	@ConditionalOnMissingBean
	public DomainService domainService(DomainClient domainClient)
	{
		return new DefaultDomainService(domainClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	public AddressService addressService(AddressClient addressClient)
	{
		return new DefaultAddressService(addressClient);
	}	

}
