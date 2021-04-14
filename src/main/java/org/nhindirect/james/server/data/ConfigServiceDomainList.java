package org.nhindirect.james.server.data;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.james.core.Domain;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.lib.AbstractDomainList;
import org.nhind.config.rest.DomainService;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.EntityStatus;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the DomainList that pulls all information from the ConfigService
 * This should guarantee that any additions made using the config UI will be available
 * from James.  Conversely, additions made using the James web admin REST API should
 * be reflected in the Config UI
 * @author Greg Meyer
 * @since 6.0.1
 */
public class ConfigServiceDomainList extends AbstractDomainList
{
	protected final DomainService domService;
	
	@Inject
	public ConfigServiceDomainList(DNSService dns, DomainService domService)
	{
		super(dns);
		
		this.domService = domService;
	}
	
    @Override
    public Domain getDefaultDomain() throws DomainListException 
    {
    	// we really don't use the concept of a default domain
    	// so just return some place holder
    	return Domain.of("domain.com");
    }
	
	@Override
	protected boolean containsDomainInternal(Domain domain) throws DomainListException 
	{
		try
		{
			return !domService.searchDomains(domain.name(), null).isEmpty();
		}
		catch (ServiceException e)
		{
			throw new DomainListException("Failed to determine if domain exists.", e);
		}
	}

	@Override
	public void addDomain(Domain domain) throws DomainListException 
	{
		try
		{
            if (containsDomainInternal(domain)) 
                throw new DomainListException(domain.name() + " already exists.");
         
			
            final Address addr = new Address();
            addr.setStatus(EntityStatus.ENABLED);
            addr.setDisplayName("Postmaster");
            addr.setEmailAddress("postmaster@" + domain.name());
            addr.setEndpoint("postmaster@" + domain.name());
            addr.setDomainName(domain.name());

            final org.nhindirect.config.model.Domain newDom = new org.nhindirect.config.model.Domain();
            newDom.setDomainName(domain.name());
            newDom.setStatus(EntityStatus.ENABLED);
            newDom.setPostmasterAddress(null);
            newDom.setAddresses(Collections.singleton(addr));
            
			domService.addDomain(newDom);
		}
		catch (ServiceException e)
		{
			throw new DomainListException("Unable to add domain " + domain.name(), e);
		}
		
	}

	@Override
	public void removeDomain(Domain domain) throws DomainListException 
	{
		try
		{
			if (containsDomainInternal(domain)) 
				throw new DomainListException(domain.name() + " was not found.");
			
			domService.deleteDomain(domain.name());
		}
		catch (ServiceException e)
		{
			throw new DomainListException("Unable to remove domain " + domain.name(), e);
		}
		
	}

	@Override
	protected List<Domain> getDomainListInternal() throws DomainListException 
	{
		try
		{
			final List<Domain> domains = domService.searchDomains("", null)
			.stream()
			.map(dom -> Domain.of(dom.getDomainName()))
			.collect(Guavate.toImmutableList());
			
			return ImmutableList.copyOf(domains);
		}
		catch (ServiceException e)
		{
			throw new DomainListException("Unable to retrieve domains", e);
		}
	}

	/*
	 * The super class implementation pull domains based of local host name
	 * and IP.  We only want domains in the config service.
	 */
    @Override
    public ImmutableList<Domain> getDomains() throws DomainListException 
    {
        return ImmutableList.copyOf(getDomainListInternal());

    }

}
