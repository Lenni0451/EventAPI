package net.lenni0451.eventapi.manager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import net.lenni0451.eventapi.events.IEvent;
import net.lenni0451.eventapi.injection.IInjectionPipeline;
import net.lenni0451.eventapi.injection.IReflectedListener;
import net.lenni0451.eventapi.listener.IErrorListener;
import net.lenni0451.eventapi.listener.IEventListener;
import net.lenni0451.eventapi.reflection.EventTarget;

public class InjectionEventManager {

	private static final Map<Class<? extends IEvent>, IInjectionPipeline> EVENT_PIPELINE = new ConcurrentHashMap<>();
	private static final Map<Class<? extends IEvent>, IEventListener[]> EVENT_LISTENER = new ConcurrentHashMap<>();
	private static final List<IErrorListener> ERROR_LISTENER = new CopyOnWriteArrayList<>();
	
	public static IEventListener[] getListener(final Class<? extends IEvent> eventType) {
		return EVENT_LISTENER.get(eventType);
	}
	
	
	public static void call(final IEvent event) {
		if(event != null && EVENT_PIPELINE.containsKey(event.getClass())) {
			try {
				EVENT_PIPELINE.get(event.getClass()).call(event);
				if(EVENT_PIPELINE.containsKey(IEvent.class)) EVENT_PIPELINE.get(IEvent.class).call(event);
			} catch (Throwable e) {
				if(ERROR_LISTENER.isEmpty()) {
					throw new RuntimeException(e);
				} else {
					ERROR_LISTENER.forEach(errorListener -> errorListener.catchException(e));
				}
			}
		}
	}
	
	
	public static <T extends IEventListener> void register(final T listener) {
		register(IEvent.class, listener);
	}
	
	public static void register(final Object listener) {
		ClassPool cp = ClassPool.getDefault();
		
		for(Method method : listener.getClass().getMethods()) {
			if(!method.isAnnotationPresent(EventTarget.class)) {
				continue;
			}
			
			Class<?>[] methodArguments = method.getParameterTypes();
			if(methodArguments.length == 1 && IEvent.class.isAssignableFrom(methodArguments[0]) && Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
				method.setAccessible(true);
				
				Class<? extends IEvent> eventType = (Class<? extends IEvent>) methodArguments[0];
				String methodName = method.getName();
				
				CtClass newListener = cp.makeClass("InjectionListener_" + System.nanoTime());
				try {
					newListener.addInterface(cp.get(IReflectedListener.class.getName()));
				} catch (Throwable e) {
					throw new RuntimeException("Class could not implement IReflectedListener", e);
				}
				
				try {
					newListener.addField(CtField.make("private final " + listener.getClass().getName() + " instance;", newListener));
				} catch (Exception e) {
					throw new RuntimeException("Could not add global variables to class", e);
				}
				
				try {
					CtConstructor construct = CtNewConstructor.make("public " + newListener.getName() + "(" + listener.getClass().getName() + " ob) {this.instance = ob;}", newListener);
					newListener.addConstructor(construct);
				} catch (Throwable e) {
					throw new RuntimeException("Could not create new constructor", e);
				}
				
				StringBuilder sourceBuilder = new StringBuilder().append("{");
				sourceBuilder.append("this.instance." + methodName + "((" + eventType.getName() + ") $1);");
				sourceBuilder.append("}");
				
				try {
					CtMethod onEventMethod = CtNewMethod.make(CtClass.voidType, cp.get(IEventListener.class.getName()).getDeclaredMethods()[0].getName(), new CtClass[] {cp.get(IEvent.class.getName())}, new CtClass[] {cp.get(Throwable.class.getName())}, sourceBuilder.toString(), newListener);
					newListener.addMethod(onEventMethod);
				} catch (Throwable e) {
					throw new RuntimeException("Could not create new on event method", e);
				}
				try {
					CtMethod onEventMethod = CtNewMethod.make(cp.get(Object.class.getName()), cp.get(IReflectedListener.class.getName()).getDeclaredMethods()[0].getName(), new CtClass[0], new CtClass[0], "{return this.instance;}", newListener);
					newListener.addMethod(onEventMethod);
				} catch (Exception e) {
					throw new RuntimeException("Could not create new get instance method", e);
				}
				
				Class<?> newListenerClass;
				try {
					newListenerClass = newListener.toClass();
				} catch (Throwable e) {
					throw new RuntimeException("Could not compile class", e);
				}
				Object listenerObject;
				try {
					listenerObject = newListenerClass.getConstructors()[0].newInstance(listener);
				} catch (Throwable e) {
					throw new RuntimeException("Could not instantiate new class", e);
				}
				
				register(eventType, (IEventListener) listenerObject);
			}
		}
	}
	
	public static <T extends IEventListener> void register(final Class<? extends IEvent> eventType, final T listener) {
		IEventListener[] eventListener = EVENT_LISTENER.computeIfAbsent(eventType, c -> new IEventListener[0]);
		IEventListener[] newEventListener = new IEventListener[eventListener.length + 1];
		
		EVENT_LISTENER.put(eventType, newEventListener);
		
		for(int i = 0; i <= eventListener.length; i++) {
			if(i != eventListener.length) {
				newEventListener[i] = eventListener[i];
			} else {
				newEventListener[i] = listener;
			}
		}
		
		EVENT_PIPELINE.put(eventType, rebuildPipeline(newEventListener));
	}
	
	
	public static void unregister(final Object listener) {
		for(Map.Entry<Class<? extends IEvent>, IEventListener[]> entry : EVENT_LISTENER.entrySet()) {
			IEventListener[] eventListener = EVENT_LISTENER.computeIfAbsent(entry.getKey(), c -> new IEventListener[0]);
			IEventListener[] newEventListener = new IEventListener[eventListener.length - 1];
			
			entry.setValue(newEventListener);
			
			for(int i = 0, x = 0; i < eventListener.length; i++) {
				if(!eventListener[i].equals(listener) && (eventListener[i] instanceof IReflectedListener && !((IReflectedListener) eventListener[i]).getInstance().equals(listener))) {
					newEventListener[x] = eventListener[i];
					x++;
				}
			}

			EVENT_PIPELINE.put(entry.getKey(), rebuildPipeline(newEventListener));
		}
	}
	
	
	public static void addErrorListener(final IErrorListener errorListener) {
		if(!ERROR_LISTENER.contains(errorListener))
			ERROR_LISTENER.add(errorListener);
	}
	
	public static boolean removeErrorListener(final IErrorListener errorListener) {
		return ERROR_LISTENER.remove(errorListener);
	}
	
	
	private static IInjectionPipeline rebuildPipeline(final IEventListener[] eventListener) {
		ClassPool cp = ClassPool.getDefault();
		
		String methodName = null;
		try {
			for(CtMethod method : cp.get(InjectionEventManager.class.getName()).getDeclaredMethods()) {
				if(method.getReturnType().getSimpleName().equals(cp.get(IEventListener[].class.getName()).getSimpleName())) {
					methodName = method.getName();
					break;
				}
			}
			if(methodName == null) {
				throw new NullPointerException();
			}
		} catch (Throwable e) {
			throw new IllegalStateException("Could not find method name to get listener array", e);
		}
		
		StringBuilder sourceBuilder = new StringBuilder().append("{");
		sourceBuilder.append(IEventListener.class.getName() + "[] listener = " + InjectionEventManager.class.getName() + "." + methodName + "($1.getClass());");
		
		for(int i = 0; i < eventListener.length; i++) {
			try {
				sourceBuilder.append("listener[" + i + "]." + cp.get(IEventListener.class.getName()).getDeclaredMethods()[0].getName() + "($1);");
			} catch (NotFoundException e) {
				if(ERROR_LISTENER.isEmpty()) {
					throw new RuntimeException(e);
				} else {
					ERROR_LISTENER.forEach(errorListener -> errorListener.catchException(e));
				}
			}
		}
		sourceBuilder.append("}");
		
		CtClass newPipeline = cp.makeClass("InjectionPipeline_" + System.nanoTime());
		try {
			newPipeline.addInterface(cp.get(IInjectionPipeline.class.getName()));
		} catch (Throwable e) {
			throw new RuntimeException("Class could not implement IInjectionPipeline", e);
		}
		
		CtMethod method;
		try {
			method = CtNewMethod.make(CtClass.voidType, cp.get(IInjectionPipeline.class.getName()).getDeclaredMethods()[0].getName(), new CtClass[] {cp.get(IEvent.class.getName())}, new CtClass[] {cp.get(Throwable.class.getName())}, sourceBuilder.toString(), newPipeline);
		} catch (Throwable e) {
			throw new RuntimeException("Could not create new call method", e);
		}
		
		try {
			newPipeline.addMethod(method);
		} catch (Throwable e) {
			throw new RuntimeException("Could not add call method to class", e);
		}
		
		Class<? extends IInjectionPipeline> pipelineClass;
		try {
			pipelineClass = (Class<? extends IInjectionPipeline>) newPipeline.toClass();
		} catch (Throwable e) {
			throw new RuntimeException("Could not compile class", e);
		}
		IInjectionPipeline pipelineObject;
		try {
			pipelineObject = pipelineClass.newInstance();
		} catch (Throwable e) {
			throw new RuntimeException("Could not instantiate new class", e);
		}
		return pipelineObject;
	}
	
}
