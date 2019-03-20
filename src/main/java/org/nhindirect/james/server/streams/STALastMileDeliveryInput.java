package org.nhindirect.james.server.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface STALastMileDeliveryInput
{
    public static final String STA_LAST_MILE_INPUT = "direct-sta-last-mile-input";
    
    @Input(STA_LAST_MILE_INPUT)
	SubscribableChannel staLastMileInput();
}
