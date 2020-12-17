package test.lenni0451;

import net.lenni0451.eventapi.events.EventTarget;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.manager.ASMEventManager;

public class Test {

    public static void main(String[] args) {
        ASMEventManager.register(Test.class);
        ASMEventManager.call(new NiceEvent());
    }

    public static class NiceEvent implements IEvent {
    }

    @EventTarget
    public static void staticEventListener(NiceEvent event) {
        System.out.println("static " + event);
    }

    @EventTarget
    public void lel(NiceEvent event) {
        System.out.println("lel " + event);
    }

}
