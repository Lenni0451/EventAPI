package test.lenni0451;

import java.text.DecimalFormat;
import java.util.HashMap;

import net.lenni0451.eventapi.EventManager;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.events.premade.EventCancellable;
import net.lenni0451.eventapi.listener.IErrorListener;
import net.lenni0451.eventapi.listener.IEventListener;
import net.lenni0451.eventapi.reflection.EventTarget;

public class Main {

	public static void main(String[] args) {
		EventManager.register(new Evlistener());
		EventManager.addErrorListener(new IErrorListener() {
			@Override
			public void catchException(Throwable exception) {
				exception.printStackTrace();
			}
		});

		DecimalFormat df = new DecimalFormat();
		float out = 0;
		for(int i = 0; i < 1000000; i++) {
			long start = System.nanoTime();
			
			EventManager.call(new Event1());
			
			out += System.nanoTime() - start;
		}
		System.out.println(df.format(out / 1000000));
	}
	
	public static class Event1 implements IEvent {}
	public static class Event2 extends EventCancellable {}

	public static class Evlistener implements IEventListener {

		@EventTarget
		public void onLowest(Event1 e) {
			this.testCode();
		}

		public void onEvent(IEvent event) {
			this.testCode();
		}
		
		public void testCode() {
			if(Math.abs(-1) == 1) {
				new HashMap<>().put("asasa", "a");
			}
		}
		
	}
	
}
