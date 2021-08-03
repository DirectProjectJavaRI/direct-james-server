package org.nhindirect.james.server.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"org.nhindirect.james.server.spring", "org.nhindirect.james.server.streams"})
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class JamesServerApplication
{
    public static void main(String[] args) 
    {
        SpringApplication springApplication = 
                new SpringApplicationBuilder()
                .sources(JamesServerApplication.class)
                .web(WebApplicationType.NONE)
                .build();

        springApplication.run(args);
    }  
}
