package org.nhindirect.james.server.modules;

import org.apache.james.webadmin.authentication.AuthenticationFilter;
import org.nhindirect.james.server.authfilter.WebAdminBasicAuthFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Replaces James' default webadmin authentication filter (JWT/Password header/none) with HTTP Basic authentication.
 * This module must be applied as an override of the James webadmin modules.
 */
public class WebAdminBasicAuthModule extends AbstractModule
{
	private final String user;

	private final String password;

	public WebAdminBasicAuthModule(String user, String password)
	{
		this.user = user;
		this.password = password;
	}

    @Provides
    @Singleton
    AuthenticationFilter provideAuthenticationFilter()
    {
        return new WebAdminBasicAuthFilter(user, password);
    }
}
