package org.nhindirect.james.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.DomainService;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan({"org.nhindirect.james.server.spring", "org.nhindirect.james.server.streams"})
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class TestApplication
{
	@Value("${james.server.webadmin.username}")
	protected String webAdminUser;
	
	@Value("${james.server.webadmin.password}")
	protected String webAdminPass;
	
	@Value("${james.server.test.domain}")
	protected String testDomainName;
	
    public static void main(String[] args) 
    {
    	
		/*
		 * Clean up James files
		 */
		try
		{
			FileUtils.deleteDirectory(new File("conf"));
			FileUtils.deleteDirectory(new File("var"));
		}
		catch (Exception e) {/* no-op */}
		
        SpringApplication springApplication = 
                new SpringApplicationBuilder()
                .sources(TestApplication.class)
                .web(WebApplicationType.NONE)
                .build();

        springApplication.run(args);
    }  
    
    @Bean
    @Primary
	public DomainService mockDomainService() throws Exception
	{
    	final DomainService domainService = mock(DomainService.class);
    	
		final Domain testDomain = new Domain();
		testDomain.setDomainName(testDomainName);
		testDomain.setStatus(EntityStatus.ENABLED);
		
		when(domainService.searchDomains("", null)).thenReturn(Arrays.asList(testDomain));
    	
    	return domainService;
	}
    
	@Bean
	@Primary
	public AddressService mockAddressService()
	{
    	final AddressService addressService = mock(AddressService.class);
    	
    	return addressService;
	}
	
	@Bean
	public RestTemplate webAdminRestTemplate()
	{
		final RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(webAdminUser, webAdminPass));
		
		return restTemplate;
	}
}
