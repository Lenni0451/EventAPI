package net.lenni0451.eventapi;

import net.lenni0451.eventapi.events.EventPriority;
import net.lenni0451.eventapi.listener.IEventListener;

class EventExecutor {
	
	private final IEventListener eventListener;
	private final EventPriority priority;
	
	public EventExecutor(final IEventListener eventListener, final EventPriority priority) {
		this.eventListener = eventListener;
		this.priority = priority;
	}
	
	public IEventListener getEventListener() {
		return this.eventListener;
	}
	
	public EventPriority getPriority() {
		return this.priority;
	}
	
}
