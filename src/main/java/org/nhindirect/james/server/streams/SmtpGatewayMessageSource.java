package org.nhindirect.james.server.streams;


import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class SmtpGatewayMessageSource
{	
	protected static SmtpGatewayMessageSource messageSourceInstance;
	
	// Maps to the Spring Cloud Stream functional output binding name.
	protected static final String OUT_BINDING_NAME = "direct-smtp-gateway-message-out-0";
	
	@Autowired
	private StreamBridge streamBridge;
	
	public SmtpGatewayMessageSource()
	{
		messageSourceInstance = this;
	}
	
	public <T> void forwardSMTPMessage(SMTPMailMessage msg) 
	{
		streamBridge.send(OUT_BINDING_NAME, SMTPMailMessageConverter.toStreamMessage(msg));
	}

	public static SmtpGatewayMessageSource getMessageSourceInstance()
	{
		return messageSourceInstance;
	}
}
