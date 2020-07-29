package net.lenni0451.eventapi.injection.asm;

import net.lenni0451.eventapi.events.IEvent;

public interface IInjectionEventHandler {

    void call(final IEvent event);

}
