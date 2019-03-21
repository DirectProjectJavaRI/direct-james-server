package org.nhindirect.james.server.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface SmtpGatewayMessageOutput
{
	public static final String SMTP_GATEWAY_MESSAGE_OUTPUT = "direct-smtp-mq-gateway";
	
	@Output(SMTP_GATEWAY_MESSAGE_OUTPUT)
	MessageChannel txOutput();
}
