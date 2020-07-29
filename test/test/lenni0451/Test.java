package test.lenni0451;

import net.lenni0451.eventapi.events.EventTarget;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.manager.ASMEventManager;

public class Test {

    public static void main(String[] args) {
        TestListener listener = new TestListener();
        ASMEventManager.register(listener);
        ASMEventManager.register(listener);
        ASMEventManager.register(listener);
        ASMEventManager.register(listener);
        ASMEventManager.call(new TestEvent());
        ASMEventManager.unregister(listener);
        ASMEventManager.call(new TestEvent());
    }

    public static class TestEvent implements IEvent {}

    public static class TestListener {
        @EventTarget
        public void test(TestEvent event) {
            System.out.println("Hi Kevin!");
        }
    }

}
