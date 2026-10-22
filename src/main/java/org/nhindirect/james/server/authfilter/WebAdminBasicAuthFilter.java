package org.nhindirect.james.server.authfilter;

import static spark.Spark.halt;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.apache.james.webadmin.authentication.AuthenticationFilter;
import org.eclipse.jetty.http.HttpStatus;

import spark.Request;
import spark.Response;

public class WebAdminBasicAuthFilter implements AuthenticationFilter
{
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    public static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";
    public static final String AUTHENTICATE_HEADER_NAME = "WWW-Authenticate";
    public static final String AUTHENTICATE_HEADER_VALUE = "Basic realm=\"James WebAdmin\"";
    public static final String OPTIONS = "OPTIONS";

	protected final String user;
	protected final byte[] passHash;

	public WebAdminBasicAuthFilter(String user, String pass)
	{
		this.user = user;
		this.passHash = hash(pass);
	}

    @Override
    public void handle(Request request, Response response) throws Exception
    {
        if (!OPTIONS.equals(request.requestMethod()))
        {
            Optional<String> basicAuth = Optional.ofNullable(request.headers(AUTHORIZATION_HEADER_NAME))
                .filter(value -> value.startsWith(AUTHORIZATION_HEADER_PREFIX))
                .map(value -> value.substring(AUTHORIZATION_HEADER_PREFIX.length()));

            checkHeaderPresent(response, basicAuth);

            // limit the split to 2 so passwords containing ':' are preserved
            final String[] subjectAndSecret = new String(Base64.decodeBase64(basicAuth.get()), Charset.defaultCharset()).split(":", 2);
            if (subjectAndSecret.length != 2)
                reject(response, "Invalid Basic Auth header.");

            checkValidPass(response, subjectAndSecret[1]);
            checkIsAdmin(response, subjectAndSecret[0]);
        }
    }

    private static byte[] hash(String value)
    {
		try
		{
			// MessageDigest instances are not thread safe, so use a new one for each hash
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(Charset.defaultCharset()));
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Error creating webadmin password hash: " + e.getMessage());
		}
    }

    private void reject(Response response, String message)
    {
        response.header(AUTHENTICATE_HEADER_NAME, AUTHENTICATE_HEADER_VALUE);
        halt(HttpStatus.UNAUTHORIZED_401, message);
    }

    private void checkHeaderPresent(Response response, Optional<String> basicAuth)
    {
        if (!basicAuth.isPresent())
            reject(response, "No Basic Auth header.");
    }

    private void checkIsAdmin(Response response, String user)
    {
        if (user.compareToIgnoreCase(this.user) != 0)
            reject(response, "Non authorized user.");
    }

    private void checkValidPass(Response response, String pass)
    {
        // constant time comparison
        if (!MessageDigest.isEqual(hash(pass), this.passHash))
            reject(response, "Invalid login.");
    }
}
