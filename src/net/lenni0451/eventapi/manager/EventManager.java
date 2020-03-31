package net.lenni0451.eventapi.manager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.lenni0451.eventapi.events.EventPriority;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.events.types.IStoppable;
import net.lenni0451.eventapi.listener.IErrorListener;
import net.lenni0451.eventapi.listener.IEventListener;
import net.lenni0451.eventapi.reflection.EventTarget;
import net.lenni0451.eventapi.reflection.ReflectedEventListener;

/**
 * This EventManager type has the most features but is not the fastest.<br>
 * It is perfect for average usage which does not require the most performance.<br>
 * For faster speeds use the {@link InjectEventManager} or the {@link MinimalEventManager}
 * 
 * @author Lenni0451
 */
public class EventManager {
	
	private static final Map<Class<? extends IEvent>, List<EventExecutor>> EVENT_LISTENER = new ConcurrentHashMap<>();
	private static final List<IErrorListener> ERROR_LISTENER = new CopyOnWriteArrayList<>();
	
	public static void call(final IEvent event) {
		if(event == null) return;
		
		List<EventExecutor> eventListener = new ArrayList<>();
		if(EVENT_LISTENER.containsKey(event.getClass())) eventListener.addAll(EVENT_LISTENER.get(event.getClass()));
		if(EVENT_LISTENER.containsKey(IEvent.class)) eventListener.addAll(EVENT_LISTENER.get(IEvent.class));

		for(EventExecutor listener : eventListener) {
			try {
				listener.getEventListener().onEvent(event);
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

	
	public static <T extends IEventListener> void register(final T listener) {
		register(IEvent.class, EventPriority.MEDIUM, listener);
	}
	
	public static void register(final Object listener) {
		for(Method method : listener.getClass().getMethods()) {
			if(!method.isAnnotationPresent(EventTarget.class)) {
				continue;
			}
		
			EventTarget anno = method.getAnnotation(EventTarget.class);
			Class<?>[] methodArguments = method.getParameterTypes();
			if(methodArguments.length == 1 && IEvent.class.isAssignableFrom(methodArguments[0])) {
				ReflectedEventListener eventListener = new ReflectedEventListener(listener, methodArguments[0], method);
				if(methodArguments[0].equals(IEvent.class)) {
					register(anno.priority(), eventListener);
				} else {
					register((Class<? extends IEvent>) methodArguments[0], anno.priority(), eventListener);
				}
			}
		}
	}

	public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final T listener) {
		register(eventType, EventPriority.MEDIUM, listener);
	}

	public static <T extends IEventListener> void register(final byte eventPriority, final T listener) {
		register(IEvent.class, eventPriority, listener);
	}
	
	public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final byte eventPriority, final T listener) {
		List<EventExecutor> eventListener = EVENT_LISTENER.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<EventExecutor>());
		eventListener.add(new EventExecutor(listener, eventPriority));
		
		for(Map.Entry<Class<? extends IEvent>, List<EventExecutor>> entry : EVENT_LISTENER.entrySet()) {
			List<EventExecutor> eventExecutor = entry.getValue();
			Collections.sort(eventExecutor, new Comparator<EventExecutor>() {
				@Override
				public int compare(EventExecutor o1, EventExecutor o2) {
					return Integer.compare(o2.getPriority(), o1.getPriority());
				}
			});
		}
	}
	
	
	public static void unregister(final Object listener) {
		for(Map.Entry<Class<? extends IEvent>, List<EventExecutor>> entry : EVENT_LISTENER.entrySet()) {
			entry.getValue().removeIf(eventExecutor -> (eventExecutor.getEventListener().equals(listener) || (eventExecutor.getEventListener() instanceof ReflectedEventListener && ((ReflectedEventListener) eventExecutor.getEventListener()).getCallInstance().equals(listener))));
		}
	}

	
	public static void addErrorListener(final IErrorListener errorListener) {
		if(!ERROR_LISTENER.contains(errorListener))
			ERROR_LISTENER.add(errorListener);
	}
	
	public static boolean removeErrorListener(final IErrorListener errorListener) {
		return ERROR_LISTENER.remove(errorListener);
	}
	
}

class EventExecutor {
	
	private final IEventListener eventListener;
	private final byte priority;
	
	public EventExecutor(final IEventListener eventListener, final byte priority) {
		this.eventListener = eventListener;
		this.priority = priority;
	}
	
	public IEventListener getEventListener() {
		return this.eventListener;
	}
	
	public byte getPriority() {
		return this.priority;
	}
	
}
