package net.lenni0451.eventapi.manager;

import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.listener.IEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinimalEventManager {

    private static final Map<Class<? extends IEvent>, List<IEventListener>> EVENT_LISTENER = new ConcurrentHashMap<>();

    public static <T extends IEvent> T call(final T event) {
        if (EVENT_LISTENER.containsKey(event.getClass())) EVENT_LISTENER.get(event.getClass()).forEach(l -> {
            try {
                l.onEvent(event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        return event;
    }

    public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final T listener) {
        EVENT_LISTENER.computeIfAbsent(eventType, c -> new CopyOnWriteArrayList<IEventListener>()).add(listener);
    }

    public static <T extends IEventListener> void unregister(final T listener) {
        EVENT_LISTENER.forEach((key, value) -> value.removeIf(l -> l.equals(listener)));
    }

}
