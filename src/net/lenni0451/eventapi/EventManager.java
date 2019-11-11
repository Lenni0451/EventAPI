package net.lenni0451.eventapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lenni0451.eventapi.events.EventPriority;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.events.types.IStoppable;
import net.lenni0451.eventapi.listener.IErrorListener;
import net.lenni0451.eventapi.listener.IEventListener;
import net.lenni0451.eventapi.reflection.EventTarget;
import net.lenni0451.eventapi.reflection.ReflectedEventListener;

public class EventManager {
	
	private static final Map<Class<? extends IEvent>, List<IEventListener>> EVENT_LISTENER = new HashMap<>();
	private static final List<IErrorListener> ERROR_LISTENER = new ArrayList<>();
	
	public static void call(final IEvent event) {
		List<IEventListener> eventListener = new ArrayList<>(EVENT_LISTENER.get(event.getClass()));
		if(EVENT_LISTENER.containsKey(null))
			eventListener.addAll(EVENT_LISTENER.get(null));
		
		if(eventListener != null) {
			for(IEventListener listener : eventListener) {
				try {
					listener.onEvent(event);
				} catch (Throwable e) {
					if(ERROR_LISTENER.isEmpty()) {
						throw new RuntimeException(e);
					} else {
						ERROR_LISTENER.forEach(errorListener -> errorListener.catchException(e));
					}
				}
				if(event instanceof IStoppable && ((IStoppable) event).isStopped()) {
					break;
				}
			}
		}
	}

	public static <T extends IEventListener> void register(final T listener) {
		register(null, EventPriority.MEDIUM, listener);
	}
	
	public static void register(final Object listener) {
		for(Method method : listener.getClass().getMethods()) {
			EventTarget anno = method.getDeclaredAnnotation(EventTarget.class);
			if(anno != null) {
				Class<?>[] methodArguments = method.getParameterTypes();
				if(methodArguments.length == 1 && IEvent.class.isAssignableFrom(methodArguments[0])) {
					ReflectedEventListener eventListener = new ReflectedEventListener(listener, methodArguments[0], method);
					register(anno.priority(), eventListener);
				}
			}
		}
	}

	public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final T listener) {
		register(eventType, EventPriority.MEDIUM, listener);
	}

	public static <T extends IEventListener> void register(final EventPriority eventPriority, final T listener) {
		register(null, eventPriority, listener);
	}
	
	public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final EventPriority eventPriority, final T listener) {
		List<IEventListener> eventListener = EVENT_LISTENER.computeIfAbsent(eventType, k -> new ArrayList<IEventListener>());
		eventListener.add(listener);
		
		//TODO: Handle event priority
	}
	
	public static <T extends IEventListener> void unregister(final T listener) {
		for(Map.Entry<Class<? extends IEvent>, List<IEventListener>> entry : EVENT_LISTENER.entrySet()) {
			entry.getValue().removeIf(eventListener -> eventListener.equals(listener));
		}
	}
	
	public static void unregister(final Object listener) {
		for(Map.Entry<Class<? extends IEvent>, List<IEventListener>> entry : EVENT_LISTENER.entrySet()) {
			entry.getValue().removeIf(eventListener -> {
				return eventListener.equals(listener) || (eventListener instanceof ReflectedEventListener && ((ReflectedEventListener) eventListener).getCallInstance().equals(listener));
			});
		}
	}
	
}
