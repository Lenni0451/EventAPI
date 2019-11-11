package net.lenni0451.eventapi.reflection;

import java.lang.reflect.Method;

import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.listener.IEventListener;

public class ReflectedEventListener implements IEventListener {
	
	private final Object callInstance;
	private final Class<?> eventClass;
	private final Method method;
	
	public ReflectedEventListener(final Object callInstance, final Class<?> eventClass, final Method method) {
		this.callInstance = callInstance;
		this.eventClass = eventClass;
		this.method = method;
	}
	
	public Object getCallInstance() {
		return this.callInstance;
	}
	
	@Override
	public void onEvent(IEvent event) {
		try {
			method.invoke(this.callInstance, new Object[] {eventClass.cast(event)});
		} catch (Throwable e) {
			throw new RuntimeException("Error invoking reflected method", e);
		}
	}
	
}
