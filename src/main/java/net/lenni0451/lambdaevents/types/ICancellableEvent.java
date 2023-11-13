package net.lenni0451.lambdaevents.types;

import net.lenni0451.lambdaevents.EventHandler;

/**
 * An interface to mark an event as cancellable.<br>
 * Required for {@link EventHandler#handleCancelled()} to work.
 */
public interface ICancellableEvent {

    /**
     * @return If the event is cancelled
     */
    boolean isCancelled();

}
