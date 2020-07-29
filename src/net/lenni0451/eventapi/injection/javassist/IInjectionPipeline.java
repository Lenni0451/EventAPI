package net.lenni0451.eventapi.injection.javassist;

import net.lenni0451.eventapi.events.IEvent;

public interface IInjectionPipeline {
	
	void call(IEvent event);
	
}
