package net.lenni0451.eventapi.events;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface EventTarget {
	
	byte priority() default EventPriority.MEDIUM;
	
}
