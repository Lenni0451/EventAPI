package net.lenni0451.eventapi.events.premade;

import net.lenni0451.eventapi.events.types.ICancellable;
import net.lenni0451.eventapi.events.IEvent;

public class EventCancellable implements IEvent, ICancellable {
	
	private boolean cancelled = false;
	
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(final boolean cancelled) {
		this.cancelled = cancelled;
	}
	
}
