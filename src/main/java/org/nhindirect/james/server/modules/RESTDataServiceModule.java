package org.nhindirect.james.server.modules;

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
	
	public RESTDataServiceModule(DomainService domService)
	{
		this.domService = domService;
	}
	
    @Override
    protected void configure() 
    {
        bind(DomainService.class).toInstance(domService);
    }
}
