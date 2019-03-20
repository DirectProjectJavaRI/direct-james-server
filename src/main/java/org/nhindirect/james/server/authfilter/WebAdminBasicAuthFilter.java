package org.nhindirect.james.server.authfilter;

import static spark.Spark.halt;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Objects;
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
    public static final String OPTIONS = "OPTIONS";
    
	protected final String user;
	protected final byte[] passHash;
	protected final MessageDigest digest;
	
	public WebAdminBasicAuthFilter(String user, String pass)
	{
		try
		{
			digest = MessageDigest.getInstance("SHA-256");
		
			this.user = user;
			this.passHash = digest.digest(pass.getBytes(Charset.defaultCharset()));
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Error creating webadmin password hash: " + e.getMessage());
		}
	}
	
    @Override
    public void handle(Request request, Response response) throws Exception 
    {
        if (request.requestMethod() != OPTIONS) 
        {
            Optional<String> basicAuth = Optional.ofNullable(request.headers(AUTHORIZATION_HEADER_NAME))
                .filter(value -> value.startsWith(AUTHORIZATION_HEADER_PREFIX))
                .map(value -> value.substring(AUTHORIZATION_HEADER_PREFIX.length()));

            checkHeaderPresent(basicAuth);
            
            final String[] subjectAndSecret = new String(Base64.decodeBase64(basicAuth.get()), Charset.defaultCharset()).split(":");
            final String user = subjectAndSecret[0];
            final String pass = subjectAndSecret[1];
            
            checkValidPass(pass);
            checkIsAdmin(user);            
        }
    }
    
    private void checkHeaderPresent(Optional<String> basicAuth) 
    {
        if (!basicAuth.isPresent()) 
        {
            halt(HttpStatus.UNAUTHORIZED_401, "No Basic Auth header.");
        }
    }
    
    private void checkIsAdmin(String user) 
    {
        if (user.compareToIgnoreCase(this.user) != 0)
        {
            halt(HttpStatus.UNAUTHORIZED_401, "Non authorized user.");
        }
    }
    
    private void checkValidPass(String pass) 
    {
    	final byte[] passDigest = digest.digest(pass.getBytes(Charset.defaultCharset()));
    	
        if (!Objects.deepEquals(passDigest, this.passHash))
        {
            halt(HttpStatus.UNAUTHORIZED_401, "Invalid login.");
        }
    }    
}
