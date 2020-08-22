# EventAPI
A feature rich and easy to use EventAPI for Java.  
It has 4 different EventManager types:
 - EventManager
	 - The most common, Reflection based EventManager.
 - InjectionEventManager
	 - An EventManager uses Javassist to generate the Event pipelines and listeners during runtime. It was the fastest until the ASMEventManager was born.
 - MinimalEventManager
	 - It uses Interfaces for handling event calls. It is actually faster than the InjectionEventManager but has no features at all. It is the best one if you don't really care about features but need maximum speed.
	 - This no longer has a real point of existing since if you want speed you should use the ASMEventManager.
 - ASMEventManager
     - This EventManager type uses ASM to generate the call pipeline. It has super high speeds and all features. This is the fastest we can get without extremely hacky stuff like replacing classes in ram during runtime. This is the best one now.

## Using it
First you need to download and implement the library into your project (obviously).  
Then you should choose the event manager type you want. You can even use all variants at the same time. It does'nt matter.  
You register an event listener by calling the ``EventManager.register();`` method.  
To call an event use the ``EventManager.call();`` method. All the event manager types are thread safe. So don't worry about multiple events in different threads.

## Contributing
If you want to you can help improving this lib. If you find a bug but don't know how to fix it or simply don't want to, you can simply create an issue so I can fix it (And please check if there is already an issue created with the same bug/feature request/...).  
Thank you.
