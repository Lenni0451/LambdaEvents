package net.lenni0451.le;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    byte priority() default 0;

    /**
     * Not needed for event handler methods<br>
     * Only for {@link Consumer} fields in classes
     */
    Class<?>[] eventClasses() default {};

}
