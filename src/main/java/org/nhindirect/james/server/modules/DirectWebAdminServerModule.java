package org.nhindirect.james.server.modules;

import static org.apache.james.webadmin.WebAdminConfiguration.DISABLED_CONFIGURATION;

import java.io.FileNotFoundException;
import java.util.Optional;

import org.apache.commons.configuration.Configuration;
import org.apache.james.modules.server.HealthCheckRoutesModule;
import org.apache.james.modules.server.TaskRoutesModule;
import org.apache.james.modules.server.WebAdminServerModule.WebAdminServerModuleConfigurationPerformer;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.GuiceProbe;
import org.apache.james.utils.PropertiesProvider;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.FixedPortSupplier;
import org.apache.james.webadmin.TlsConfiguration;
import org.apache.james.webadmin.WebAdminConfiguration;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.authentication.AuthenticationFilter;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.apache.james.webadmin.utils.JsonTransformerModule;
import org.nhindirect.james.server.authfilter.WebAdminBasicAuthFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectWebAdminServerModule extends AbstractModule
{

    private static final boolean DEFAULT_DISABLED = true;
    private static final String DEFAULT_NO_CORS_ORIGIN = null;
    private static final boolean DEFAULT_CORS_DISABLED = false;
    private static final String DEFAULT_NO_KEYSTORE = null;
    private static final boolean DEFAULT_HTTPS_DISABLED = false;
    private static final String DEFAULT_NO_PASSWORD = null;
    private static final String DEFAULT_NO_TRUST_KEYSTORE = null;
    private static final String DEFAULT_NO_TRUST_PASSWORD = null;	
	
    @Override
    protected void configure() 
    {
        install(new TaskRoutesModule());
        install(new HealthCheckRoutesModule());

        bind(JsonTransformer.class).in(Scopes.SINGLETON);
        bind(WebAdminServer.class).in(Scopes.SINGLETON);

        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(WebAdminServerModuleConfigurationPerformer.class);
        Multibinder.newSetBinder(binder(), GuiceProbe.class).addBinding().to(WebAdminGuiceProbe.class);
        Multibinder.newSetBinder(binder(), JsonTransformerModule.class);
    }
	
    @Provides
    public WebAdminConfiguration provideWebAdminConfiguration(PropertiesProvider propertiesProvider) throws Exception {
        try {
            Configuration configurationFile = propertiesProvider.getConfiguration("webadmin");
            return WebAdminConfiguration.builder()
                .enable(configurationFile.getBoolean("enabled", DEFAULT_DISABLED))
                .port(new FixedPortSupplier(configurationFile.getInt("port", WebAdminServer.DEFAULT_PORT)))
                .tls(readHttpsConfiguration(configurationFile))
                .enableCORS(configurationFile.getBoolean("cors.enable", DEFAULT_CORS_DISABLED))
                .urlCORSOrigin(configurationFile.getString("cors.origin", DEFAULT_NO_CORS_ORIGIN))
                .host(configurationFile.getString("host", WebAdminConfiguration.DEFAULT_HOST))
                .build();
        } catch (FileNotFoundException e) {
            log.info("No webadmin.properties file. Disabling WebAdmin interface.");
            return DISABLED_CONFIGURATION;
        }
    }
    
    @Provides
    public AuthenticationFilter providesAuthenticationFilter(PropertiesProvider propertiesProvider) throws Exception 
	{
    	Configuration configurationFile = propertiesProvider.getConfiguration("webadmin");
    	
        return new WebAdminBasicAuthFilter(configurationFile.getString("username"), configurationFile.getString("password"));
    }	
    
    private Optional<TlsConfiguration> readHttpsConfiguration(Configuration configurationFile) {
        boolean enabled = configurationFile.getBoolean("https.enabled", DEFAULT_HTTPS_DISABLED);
        if (enabled) {
            return Optional.of(TlsConfiguration.builder()
                .raw(configurationFile.getString("https.keystore", DEFAULT_NO_KEYSTORE),
                    configurationFile.getString("https.password", DEFAULT_NO_PASSWORD),
                    configurationFile.getString("https.trust.keystore", DEFAULT_NO_TRUST_KEYSTORE),
                    configurationFile.getString("https.trust.password", DEFAULT_NO_TRUST_PASSWORD))
                .build());
        }
        return Optional.empty();
    }
    
}
