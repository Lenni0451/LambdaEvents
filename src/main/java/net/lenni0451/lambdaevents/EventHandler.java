package net.lenni0451.lambdaevents;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

/**
 * Mark a method or field as an event handler.<br>
 * The method can have no parameters or one parameter which is the event.<br>
 * The field has to be a {@link Runnable} or a {@link Consumer}.<br>
 * If no event parameter is specified (methods without parameter/consumer fields), {@link EventHandler#events()} has to be specified.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * The priority of the handler.<br>
     * higher = called first<br>
     * lower = called last
     *
     * @return The priority or 0
     */
    int priority() default 0;

    /**
     * The events the handler listens to when no event parameter is present.
     *
     * @return The events or an empty array
     */
    Class<?>[] events() default {};

}
