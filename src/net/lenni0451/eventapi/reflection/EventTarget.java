package net.lenni0451.eventapi.reflection;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import net.lenni0451.eventapi.events.EventPriority;

@Retention(RUNTIME)
@Target(METHOD)
public @interface EventTarget {
	
	EventPriority priority() default EventPriority.MEDIUM;
	
}
