package net.lenni0451.eventapi.events;

public enum EventPriority {
	
	LOWEST(0),
	LOW(1),
	MEDIUM(2),
	HIGH(3),
	HIGHEST(4);
	
	
	private final int level;
	
	private EventPriority(final int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return this.level;
	}
	
}
