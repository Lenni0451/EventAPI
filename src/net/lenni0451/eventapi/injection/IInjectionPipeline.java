package net.lenni0451.eventapi.injection;

import net.lenni0451.eventapi.events.IEvent;

public interface IInjectionPipeline {
	
	public void call(IEvent event);
	
}
