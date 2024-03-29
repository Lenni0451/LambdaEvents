# LambdaEvents
Fast and modular event library for Java.

- [LambdaEvents](#lambdaevents)
  - [Releases](#releases)
    - [Gradle Template](#gradle-template)
    - [Maven Template](#maven-template)
    - [Jar File](#jar-file)
  - [Usage](#usage)
    - [LambdaManager](#lambdamanager)
    - [Generator](#generator)
    - [Events](#events)
    - [Registering](#registering)
      - [Static event handler](#static-event-handler)
      - [Virtual event handler](#virtual-event-handler)
      - [Independent event handler](#independent-event-handler)
    - [Unregistering](#unregistering)
    - [Calling](#calling)
    - [Priority](#priority)
    - [Cancelling](#cancelling)
      - [Events](#events-1)
      - [Call chain](#call-chain)
    - [Exception handling](#exception-handling)
      - [Registration](#registration)
      - [Calling](#calling-1)
    - [Event Filter](#event-filter)
  - [JMH Benchmark](#jmh-benchmark)


## Releases
To use LambdaEvents with Gradle/Maven you can follow the instructions on [maven central](https://mvnrepository.com/artifact/net.lenni0451/LambdaEvents) or my [maven server](https://maven.lenni0451.net/#/releases/net/lenni0451/LambdaEvents).
### Gradle Template
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "net.lenni0451:LambdaEvents:x.x.x"
}
```
### Maven Template
```xml
<dependency>
    <groupId>net.lenni0451</groupId>
    <artifactId>LambdaEvents</artifactId>
    <version>x.x.x</version>
</dependency>
```
You should check [maven central](https://mvnrepository.com/artifact/net.lenni0451/LambdaEvents) or my [maven server](https://maven.lenni0451.net/#/releases/net/lenni0451/LambdaEvents) for the latest version.

### Jar File
You can download the jar file from my [Jenkins server](https://build.lenni0451.net/job/LambdaEvents/).\
Since LambdaEvents has no dependencies you don't need to download any other jar files.

## Usage
### LambdaManager
To use LambdaEvents you need to create an instance of the `LambdaManager` class.\
It is the main class of the library used to register and call events.
```java
LambdaManager eventManager = LambdaManager.basic(generator);
```
Or for a thread safe version:
```java
LambdaManager eventManager = LambdaManager.threadSafe(generator);
```
There is no global instance to prevent event conflicts.

### Generator
Because of the dynamic nature of LambdaEvents you need to provide an `IGenerator` implementation.\
It is used to generate the caller which calls the event listener.\
The following implementations are provided:
 - ReflectionGenerator
 - MethodHandleGenerator
 - LambdaMetaFactoryGenerator
 - ASMGenerator (Requires [Reflect](https://github.com/Lenni0451/Reflect) and [ASM](https://mvnrepository.com/artifact/org.ow2.asm/asm))

Check out the [JMH Benchmark](#jmh-benchmark) section for performance comparisons.

The `MethodHandleGenerator` and the `LambdaMetaFactoryGenerator` have an optional `MethodHandles.Lookup` parameter.

To create your own implementation you need to implement the `IGenerator` interface.\
The `generate` method is used to generate a caller for handler which take the event as a parameter.\
The `generateVirtual` method is used to generate a caller for handler which don't take the event as a parameter.

### Events
In LambdaEvents there is no need to implement an interface or extend a class to create an event.\
You can pass any object as an event. Remember that the class is used to identify the event type. Inheritance is not checked and treated as a different event type.

### Registering
LambdaEvents has 6 different ways to listen to events:

| Type                    | Description                                                |
| ----------------------- | ---------------------------------------------------------- |
| Static methods          | A static method annotated with `@EventHandler`             |
| Virtual methods         | A virtual/non-static method annotated with `@EventHandler` |
| (static) Runnable field | A runnable field annotated with `@EventHandler`            |
| (static) Consumer field | A consumer field annotated with `@EventHandler`            |
| Independent Runnable    | A runnable which is passed to the `register` method        |
| Independent Consumer    | A consumer which is passed to the `register` method        |

All `register`/`unregister` methods have an optional `event class` parameter to specify the type of the event to register/unregister.\
This parameter is required when registering an independent event handler.

Runnable fields or Consumer fields without type parameters require the event type(s) to be added to the `@EventHandler(events = {Event.class})` annotation.\
Methods are required to not have any parameters or to have the event type as a parameter. Methods without parameters require the event type(s) to be added to the `@EventHandler(events = {Event.class})` annotation.\
Independent Runnables or Consumers require the event type(s) to be passed to the `register` method.
#### Static event handler
To register static event handler (methods and fields) you need to call the `register` method of the `LambdaManager` instance passing the owner class.
```java
eventManager.register(Example.class);
```
#### Virtual event handler
To register virtual event handler (methods and fields) you need to call the `register` method of the `LambdaManager` instance passing the owner object.
```java
eventManager.register(new Example());
```
#### Independent event handler
To register independent event handler (runnables and consumers) you need to call the `register` method of the `LambdaManager` instance passing the handler, priority and event type(s).
```java
//Runnable
Runnable handler = () -> System.out.println("called");
eventManager.register(handler, 0, Event.class);

//Consumer
Consumer<Event> handler = e -> System.out.println("called " + e);
eventManager.register(handler, 0, Event.class);
```

### Unregistering
To unregister event handler you have to call the respective `unregister` method of the `LambdaManager` instance in the same way you registered them.
```java
//Static
eventManager.unregister(Example.class);

//Virtual
eventManager.unregister(oldExampleInstance);

//Independent
eventManager.unregister(handler, Event.class);
```

You can also unregister all event handlers for a specific event type by calling the `unregisterAll` method.
```java
//Unregister all event handlers for the event type
//This includes static and virtual event handlers
eventManager.unregisterAll(Event.class);

//Unregister all event handler for the event type and the given class predicate
//In this case all handlers which extend AbstractVirtualHandler are unregistered
//This includes static and virtual event handlers
eventManager.unregisterAll(Event.class, AbstractVirtualHandler.class::isAssignableFrom);

//The unregisterAll method also has an optional boolean to only unregister static/virtual event handlers
eventManager.unregisterAll(event, predicate, true /*static*/);
```

### Calling
To call an event you need to call the `call` method of the `LambdaManager` instance passing the event object.
```java
eventManager.call(new Event());
```

### Priority
LambdaEvents supports priorities for event handlers which is used to determine the execution order.\
The higher the priority is, the earlier the event handler is called.

### Cancelling
#### Events
Since there is no requirement for an event to implement an interface or extend a class you have to implement the cancellation yourself.\
Even though it's not a requirement, it is recommended to implement the `ICancellableEvent` interface.\
Example:
```java
public class Event implements ICancellableEvent {
    private boolean cancelled = false;

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}

public class Caller {
    public static void callEvent(final LambdaManager eventManager) {
        Event event = eventManager.call(new Event());
        if (event.isCancelled()) {
            return;
        }
        //Continue the method
        //...
    }
}
```
The `ICancellableEvent` interface is used for determining if an event handler should handle a cancelled event.\
The handler can specify this by adding `handleCancelled` (default true) to the `@EventHandler` annotation.\
Example:
```java
public class Handler {
    @EventHandler
    public void handleEvent(final Event event) {
        //This handler will be called even if the event is cancelled
    }

    @EventHandler(handleCancelled = false)
    public void handleEvent(final Event event) {
        //This handler will not be called if the event is cancelled
    }
}
```
#### Call chain
To cancel the event call chain and prevent following event handlers from being executed you can throw the `StopCall.INSTANCE` exception.

### Exception handling
#### Registration
If an exception is thrown during the registration process, the exception will be thrown to the caller.
#### Calling
If an exception is thrown by an event handler, the `ExceptionHandler` of the `LambdaManager` instance will be called.\
It receives the handler, the event and the thrown exception as parameters.\
By default the `ExceptionHandler` will print the stack trace of the exception to the console (`System.err`).

### Event Filter
To make sure the `LambdaManager` only registers only the correct event types you can use the `IEventFilter`.\
It is called with the event type and a type from where the filter was called.\
The filter can return `true` to allow the registration/calling of the event or `false` to skip it. An exception can be thrown to notify the caller that the event type is not allowed.\
Check types:
| Type              | Description                                                                                             |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| CALL              | The filter was called from the `call` or the `callParents` method                                       |
| REGISTER          | The filter was called from the `register` method without an explicit event type (wildcard registration) |
| EXPLICIT_REGISTER | The filter was called from the `register` method with an explicitly specified event type                |

The `unregister` method does not call the filter.\
Example filter:
```java
public boolean check(final Class event, final CheckType checkType) {
    if (event instanceof EventBase) return true;
    if (CheckType.CALL.equals(checkType)) throw new IllegalArgumentException();
    if (CheckType.EXPLICIT_REGISTER.equals(checkType)) throw new IllegalArgumentException();
    return false;
}
```
This filter will allow any event which extends `EventBase`.\
If an invalid event is passed to the `call` method an `IllegalArgumentException` will be thrown.\
If an event is registered explicitly an `IllegalArgumentException` will be thrown.\
Any other event will be blocked without throwing an exception (silent block).

## JMH Benchmark
The Benchmark shows the average time it takes to call an event 100_000 times.\
The lower the time is, the better the call performance of the generator is.\
The tests were run using Java 17 and may vary on other Java versions.

| Benchmark                           | Mode | Cnt | Score       | Error      | Units |
| ----------------------------------- | ---- | --- | ----------- | ---------- | ----- |
| CallBenchmark.callASM               | avgt | 4   | 1253339,286 | 81976,516  | ns/op |
| CallBenchmark.callLambdaMetaFactory | avgt | 4   | 1270657,312 | 133794,400 | ns/op |
| CallBenchmark.callMethodHandles     | avgt | 4   | 1893870,724 | 569223,318 | ns/op |
| CallBenchmark.callReflection        | avgt | 4   | 1466385,654 | 256169,146 | ns/op |
