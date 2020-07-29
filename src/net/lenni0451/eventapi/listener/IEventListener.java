package net.lenni0451.eventapi.listener;

import net.lenni0451.eventapi.events.IEvent;

public interface IEventListener {
	
	void onEvent(IEvent event);
	default byte getPriority() {return 0;}
	
}
