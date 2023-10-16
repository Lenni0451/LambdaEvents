package net.lenni0451.lambdaevents.test_classes;

import net.lenni0451.lambdaevents.EventHandler;

class PrivateHandlerClass {

    public boolean handled = false;

    @EventHandler(events = String.class)
    private void handle() {
        this.handled = true;
    }

}
