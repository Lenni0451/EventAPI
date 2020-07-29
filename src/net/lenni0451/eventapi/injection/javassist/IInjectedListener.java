package net.lenni0451.eventapi.injection.javassist;

import net.lenni0451.eventapi.listener.IEventListener;

public interface IInjectedListener extends IEventListener {
	
	Object getInstance();
	
}
