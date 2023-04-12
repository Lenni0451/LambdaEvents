package net.lenni0451.benchmark;

import net.lenni0451.lambdaevents.EventHandler;
import org.openjdk.jmh.infra.Blackhole;

public class BenchmarkListener {

    @EventHandler
    public void onEvent(Blackhole event) {
        event.consume(Integer.bitCount(Integer.parseInt("123")));
    }

}
