package test.lenni0451;

import net.lenni0451.eventapi.events.EventPriority;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.events.premade.EventCancellable;
import net.lenni0451.eventapi.manager.EventManager;
import net.lenni0451.eventapi.reflection.EventTarget;

public class EventHandlerPriorityTest {

	public static void main(String[] args) throws Throwable {
		EventManager.register(new Evlistener());

		EventManager.call(new Event1());
	}
	
	public static class Event1 implements IEvent {}
	public static class Event2 extends EventCancellable {}

	public static class Evlistener {

		@EventTarget(priority = EventPriority.LOWEST)
		public void onLowest(Event1 e) {
			System.out.println(0);
		}

		@EventTarget(priority = EventPriority.LOW)
		public void onLow(Event1 e) {
			System.out.println(1);
		}

		@EventTarget(priority = -127)
		public void onLow2(Event1 e) {
			System.out.println(-127);
		}

		@EventTarget(priority = EventPriority.MEDIUM)
		public void onMedium(Event1 e) {
			System.out.println(2);
		}
		
		@EventTarget(priority = EventPriority.HIGH)
		public void onHigh(Event1 e) {
			System.out.println(3);
		}

		@EventTarget(priority = EventPriority.HIGHEST)
		public void onHighest(Event1 e) {
			System.out.println(4);
		}
		
	}
	
}
