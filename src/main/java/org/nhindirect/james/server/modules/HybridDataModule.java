package org.nhindirect.james.server.modules;

import org.apache.james.modules.data.JPAMailRepositoryModule;
import org.apache.james.modules.data.JPARecipientRewriteTableModule;
import org.apache.james.modules.data.JPAUsersRepositoryModule;

import com.google.inject.AbstractModule;

/**
 * Guice module for creating instances of the ConfigServiceDomainListModule class.
 * @author Greg Meyer
 * @since 6.0.1
 */
public class HybridDataModule extends AbstractModule
{
    @Override
    protected void configure() 
    {
        install(new JPAUsersRepositoryModule());
        install(new ConfigServiceDomainListModule());
        install(new JPARecipientRewriteTableModule());
        install(new JPAMailRepositoryModule());
    }
}
