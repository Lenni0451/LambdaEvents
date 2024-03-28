package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.types.ICancellableEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class HandleCancelledTest {

    private boolean calledDefault = false;
    private boolean calledCancelled = false;
    private boolean calledCancelledIgnored = false;

    @BeforeEach
    void reset() {
        this.calledDefault = false;
        this.calledCancelled = false;
        this.calledCancelledIgnored = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void test(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        CancellableEvent event = new CancellableEvent();
        event.setCancelled(true);
        manager.call(event);

        assertTrue(this.calledDefault);
        assertTrue(this.calledCancelled);
        assertFalse(this.calledCancelledIgnored);
    }


    @EventHandler
    public void onDefault(final CancellableEvent event) {
        this.calledDefault = true;
    }

    @EventHandler(handleCancelled = true)
    public void onCancelled(final CancellableEvent event) {
        this.calledCancelled = true;
    }

    @EventHandler(handleCancelled = false)
    public void onCancelledIgnored(final CancellableEvent event) {
        this.calledCancelledIgnored = true;
    }


    public static class CancellableEvent implements ICancellableEvent {
        private boolean cancelled;

        public void setCancelled(final boolean cancelled) {
            this.cancelled = cancelled;
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }
    }

}
