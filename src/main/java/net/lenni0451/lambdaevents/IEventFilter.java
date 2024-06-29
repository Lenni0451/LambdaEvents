package net.lenni0451.lambdaevents;

/**
 * An interface to filter events before they are registered/called.
 */
public interface IEventFilter {

    /**
     * Check if an event should be registered/called.<br>
     * If {@code false} is returned the event will be ignored.<br>
     * An exception can be thrown to signal the user that the event was invalid.<br>
     * <br>
     * Example:
     * <pre>
     *     // Only allow EventBase events.
     *     // If the event is called an exception is thrown.
     *     // If the event is tried to be 'wildcard' registered it will be ignored.
     *     // If the event is registered with an explicit type an exception is thrown.
     *     public boolean check(final Class event, final CheckType checkType) {
     *         if (event instanceof EventBase) return true;
     *         if (CheckType.CALL.equals(checkType)) throw new IllegalArgumentException();
     *         if (CheckType.EXPLICIT_REGISTER.equals(checkType)) throw new IllegalArgumentException();
     *         return false;
     *     }
     * </pre>
     *
     * @param event     The event class to check
     * @param checkType The type of the check
     * @return If the event should be registered/called
     */
    boolean check(final Class<?> event, final CheckType checkType);


    enum CheckType {
        /**
         * The event is called.
         */
        CALL,
        /**
         * The event is registered without specifying its type.
         */
        REGISTER,
        /**
         * The event is registered with an explicit type.
         */
        EXPLICIT_REGISTER,
    }

}
