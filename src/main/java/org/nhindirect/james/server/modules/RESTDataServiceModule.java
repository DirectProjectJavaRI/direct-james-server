package org.nhindirect.james.server.modules;

import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.DomainService;

import com.google.inject.AbstractModule;

/**
 * Guice module for binding a instance of the ConfigServiceDomainListModule class.
 * @author Greg Meyer
 * @since 6.0.1
 */
public class RESTDataServiceModule extends AbstractModule
{
	protected DomainService domService;
	
	protected AddressService addrService;
	
	public RESTDataServiceModule(DomainService domService, AddressService addrService)
	{
		this.domService = domService;
		this.addrService = addrService;
	}
	
    @Override
    protected void configure() 
    {
        bind(DomainService.class).toInstance(domService);
        bind(AddressService.class).toInstance(addrService);
    }
}
