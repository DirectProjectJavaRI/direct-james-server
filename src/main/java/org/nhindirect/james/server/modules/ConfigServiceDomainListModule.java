package org.nhindirect.james.server.modules;


import org.apache.james.domainlist.api.DomainList;

import org.nhindirect.james.server.data.ConfigServiceDomainList;


import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module for creating instances of the ConfigServiceDomainList class.
 * @author Greg Meyer
 * @since 6.0.1
 */
public class ConfigServiceDomainListModule extends AbstractModule 
{
    @Override
    public void configure() 
    {
        bind(ConfigServiceDomainList.class).in(Scopes.SINGLETON);
        bind(DomainList.class).to(ConfigServiceDomainList.class);
    }   
}
