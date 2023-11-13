package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventFilterTest {

    private boolean calledRegister = false;
    private boolean calledExplicitRegister = false;
    private boolean calledCall = false;
    private boolean calledString = false;
    private boolean calledInteger = false;

    @BeforeEach
    void reset() {
        this.calledRegister = false;
        this.calledExplicitRegister = false;
        this.calledCall = false;
        this.calledString = false;
        this.calledInteger = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void registered(final LambdaManager manager) {
        manager.setEventFilter((event, type) -> {
            switch (type) {
                case REGISTER:
                    this.calledRegister = true;
                    break;
                case EXPLICIT_REGISTER:
                    this.calledExplicitRegister = true;
                    break;
                case CALL:
                    this.calledCall = true;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type: " + type);
            }
            return event.equals(String.class);
        });

        manager.register(this);
        assertTrue(this.calledRegister);
        assertFalse(this.calledExplicitRegister);
        assertFalse(this.calledCall);

        manager.call("Test");
        assertTrue(this.calledCall);
        assertTrue(this.calledString);

        manager.register(Integer.class, this);
        assertTrue(this.calledExplicitRegister);
        manager.call(1);
        assertFalse(this.calledInteger);
    }


    @EventHandler
    public void string(final String s) {
        this.calledString = true;
    }

    @EventHandler
    public void integer(final Integer i) {
        this.calledInteger = true;
    }

}
