package net.lenni0451.eventapi.manager;

import net.lenni0451.eventapi.events.EventTarget;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.events.types.IStoppable;
import net.lenni0451.eventapi.injection.asm.IInjectionEventHandler;
import net.lenni0451.eventapi.listener.IErrorListener;
import net.lenni0451.eventapi.utils.ASMUtils;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.objectweb.asm.Opcodes.*;

public class ASMEventManager {

    public static ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    private static final Map<Class<? extends IEvent>, Map<Object, List<Method>>> TRACKED_LISTENER = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IEvent>, IInjectionEventHandler> EVENT_HANDLER = new ConcurrentHashMap<>();
    private static final List<IErrorListener> ERROR_LISTENER = new CopyOnWriteArrayList<>();


    public static void call(final IEvent event) {
        if (event != null && EVENT_HANDLER.containsKey(event.getClass())) {
            try {
                EVENT_HANDLER.get(event.getClass()).call(event);
                if (EVENT_HANDLER.containsKey(IEvent.class)) EVENT_HANDLER.get(IEvent.class).call(event);
            } catch (Throwable e) {
                if (ERROR_LISTENER.isEmpty()) {
                    throw new RuntimeException(e);
                } else {
                    ERROR_LISTENER.forEach(errorListener -> errorListener.catchException(e));
                }
            }
        }
    }


    public static void register(final Object eventListener) {
        List<Class<? extends IEvent>> updatedEvents = new ArrayList<>();

        for (Method method : eventListener.getClass().getDeclaredMethods()) {
            EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
            if (eventTarget != null && method.getParameterTypes().length == 1 && IEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                Class<? extends IEvent> eventClass = (Class<? extends IEvent>) method.getParameterTypes()[0];
                TRACKED_LISTENER.computeIfAbsent(eventClass, eventClazz -> new ConcurrentHashMap<>()).computeIfAbsent(eventListener, listener -> new CopyOnWriteArrayList<>()).add(method);
                updatedEvents.add(eventClass);
            }
        }

        rebuildEventHandler(updatedEvents);
    }

    public static void unregister(final Object eventListener) {
        List<Class<? extends IEvent>> updatedEvents = new ArrayList<>();

        for (Method method : eventListener.getClass().getDeclaredMethods()) {
            EventTarget eventTarget = method.getDeclaredAnnotation(EventTarget.class);
            if (eventTarget != null && method.getParameterTypes().length == 1 && IEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                Class<? extends IEvent> eventClass = (Class<? extends IEvent>) method.getParameterTypes()[0];
                TRACKED_LISTENER.computeIfAbsent(eventClass, eventClazz -> new ConcurrentHashMap<>()).remove(eventListener);
                if (TRACKED_LISTENER.get(eventClass).isEmpty()) {
                    TRACKED_LISTENER.remove(eventClass);
                    EVENT_HANDLER.remove(eventClass);
                } else {
                    updatedEvents.add(eventClass);
                }
            }
        }

        rebuildEventHandler(updatedEvents);
    }


    private static void rebuildEventHandler(final List<Class<? extends IEvent>> updatedEvents) {
        for (Class<? extends IEvent> event : updatedEvents) {
            rebuildEventHandler(event);
        }
    }

    private static void rebuildEventHandler(final Class<? extends IEvent> event) {
        for (List<Method> methods : TRACKED_LISTENER.get(event).values()) {
            methods.sort((o1, o2) -> {
                EventTarget eventTarget1 = o1.getDeclaredAnnotation(EventTarget.class);
                EventTarget eventTarget2 = o2.getDeclaredAnnotation(EventTarget.class);

                return Byte.compare(eventTarget2.priority(), eventTarget1.priority());
            });
        }

        ClassNode newHandlerNode = new ClassNode();
        newHandlerNode.visit(52, ACC_PUBLIC + ACC_SUPER, "eventhandler/" + (event.getSimpleName() + "EventHandler" + System.nanoTime()), null, "java/lang/Object", new String[]{IInjectionEventHandler.class.getName().replace(".", "/")});
        newHandlerNode.visitSource("You're too slow!.sk", null);

        {
            int index = 0;
            for (Object listener : TRACKED_LISTENER.get(event).keySet()) {
                FieldNode fieldNode = new FieldNode(ACC_PUBLIC, "listener" + index, "L" + listener.getClass().getName().replace(".", "/") + ";", null, null);
                newHandlerNode.fields.add(fieldNode);

                index++;
            }
        }

        {
            MethodNode constructorNode = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
            constructorNode.instructions.add(new VarInsnNode(ALOAD, 0));
            constructorNode.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
            constructorNode.instructions.add(new InsnNode(RETURN));

            newHandlerNode.methods.add(constructorNode);
        }

        {
            MethodNode methodNode = new MethodNode(ACC_PUBLIC, IInjectionEventHandler.class.getDeclaredMethods()[0].getName(), "(L" + IEvent.class.getName().replace(".", "/") + ";)V", null, null);
            InsnList instructions = new InsnList();

            int index = 0;
            for (Map.Entry<Object, List<Method>> entry : TRACKED_LISTENER.get(event).entrySet()) {
                for (Method method : entry.getValue()) {
                    instructions.add(new VarInsnNode(ALOAD, 0));
                    instructions.add(new FieldInsnNode(GETFIELD, newHandlerNode.name, "listener" + index, "L" + entry.getKey().getClass().getName().replace(".", "/") + ";"));
                    instructions.add(new VarInsnNode(ALOAD, 1));
                    instructions.add(new TypeInsnNode(CHECKCAST, event.getName().replace(".", "/")));
                    instructions.add(new MethodInsnNode(INVOKEVIRTUAL, entry.getKey().getClass().getName().replace(".", "/"), method.getName(), "(L" + event.getName().replace(".", "/") + ";)V"));

                    if (IStoppable.class.isAssignableFrom(event)) {
                        instructions.add(new VarInsnNode(ALOAD, 1));
                        instructions.add(new TypeInsnNode(INSTANCEOF, IStoppable.class.getName().replace(".", "/")));
                        LabelNode continueLabel = new LabelNode();
                        instructions.add(new JumpInsnNode(IFEQ, continueLabel));
                        instructions.add(new VarInsnNode(ALOAD, 1));
                        instructions.add(new TypeInsnNode(CHECKCAST, IStoppable.class.getName().replace(".", "/")));
                        instructions.add(new MethodInsnNode(INVOKEINTERFACE, IStoppable.class.getName().replace(".", "/"), IStoppable.class.getDeclaredMethods()[0].getName(), "()Z"));
                        instructions.add(new JumpInsnNode(IFEQ, continueLabel));
                        instructions.add(new InsnNode(RETURN));
                        instructions.add(continueLabel);
                        instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
                    }
                }

                index++;
            }
            instructions.add(new InsnNode(RETURN));

            methodNode.instructions = instructions;
            newHandlerNode.methods.add(methodNode);
        }

        try {
            Class<?> clazz = ASMUtils.defineClass(CLASS_LOADER, newHandlerNode);
            IInjectionEventHandler eventHandler = (IInjectionEventHandler) clazz.newInstance();

            int index = 0;
            for (Object listener : TRACKED_LISTENER.get(event).keySet()) {
                final String fieldName = "listener" + index;
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(eventHandler, listener);

                index++;
            }

            EVENT_HANDLER.put(event, eventHandler);
        } catch (Throwable t) {
            if (ERROR_LISTENER.isEmpty()) {
                throw new RuntimeException(t);
            } else {
                ERROR_LISTENER.forEach(errorListener -> errorListener.catchException(t));
            }
        }
    }


    public static void addErrorListener(final IErrorListener errorListener) {
        if (!ERROR_LISTENER.contains(errorListener))
            ERROR_LISTENER.add(errorListener);
    }

    public static boolean removeErrorListener(final IErrorListener errorListener) {
        return ERROR_LISTENER.remove(errorListener);
    }

}
