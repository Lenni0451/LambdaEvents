package net.lenni0451.lambdaevents.test_classes;

import net.lenni0451.lambdaevents.types.ICancellableEvent;

public class CancellableEvent implements ICancellableEvent {

    private boolean cancelled;

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

}
