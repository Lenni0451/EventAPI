package net.lenni0451.eventapi.injection;

import net.lenni0451.eventapi.listener.IEventListener;

public interface IReflectedListener extends IEventListener {
	
	public Object getInstance();
	
}
