package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.function.Consumer;

public class CallSpeedTest {

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) run(new PrintStream(new ByteArrayOutputStream())); //Let the JVM optimize the code
        run(System.out);
    }

    public static void run(PrintStream out) {
        DecimalFormat df = new DecimalFormat();
        EventListener listener = new EventListener();

        out.println("---------- Register Event Listener ----------");
        long registerTime;
        {
            registerTime = System.nanoTime();
            LambdaManager.g().register(listener);
            out.println("EventManager: " + df.format(System.nanoTime() - registerTime));
        }
        out.println();

        long start;
        float middle;
        out.println("---------- Single call ----------");
        {
            start = System.nanoTime();
            LambdaManager.g().call(new ExampleEvent1());
            out.println("EventManager (ExampleEvent1): " + df.format(System.nanoTime() - start));

            start = System.nanoTime();
            LambdaManager.g().call(new ExampleEvent2());
            out.println("EventManager (ExampleEvent2): " + df.format(System.nanoTime() - start));
        }
        out.println();

        out.println("---------- 1000 call ----------");
        {
            middle = 0;
            for (int i = 0; i < 1000; i++) {
                start = System.nanoTime();
                LambdaManager.g().call(new ExampleEvent1());
                middle += System.nanoTime() - start;
            }
            middle /= 1000F;
            out.println("EventManager (ExampleEvent1): " + df.format(middle));

            middle = 0;
            for (int i = 0; i < 1000; i++) {
                start = System.nanoTime();
                LambdaManager.g().call(new ExampleEvent2());
                middle += System.nanoTime() - start;
            }
            middle /= 1000F;
            out.println("EventManager (ExampleEvent2): " + df.format(middle));
        }
        out.println();

        out.println("---------- 100000 call ----------");
        {
            middle = 0;
            for (int i = 0; i < 100000; i++) {
                start = System.nanoTime();
                LambdaManager.g().call(new ExampleEvent1());
                middle += System.nanoTime() - start;
            }
            middle /= 100000F;
            out.println("EventManager (ExampleEvent1): " + df.format(middle));

            middle = 0;
            for (int i = 0; i < 100000; i++) {
                start = System.nanoTime();
                LambdaManager.g().call(new ExampleEvent2());
                middle += System.nanoTime() - start;
            }
            middle /= 100000F;
            out.println("EventManager (ExampleEvent2): " + df.format(middle));
        }
        out.println();

        out.println("---------- Unregister Event Listener ----------");
        long unregisterTime;
        {
            unregisterTime = System.nanoTime();
            LambdaManager.g().unregister(listener);
            out.println("EventManager: " + df.format(System.nanoTime() - unregisterTime));
        }
        out.println();
    }

    public static class ExampleEvent1 {
    }

    public static class ExampleEvent2 {
    }

    public static class EventListener {

        @EventHandler(eventClasses = ExampleEvent1.class)
        public final Consumer<ExampleEvent1> exampleEvent1Consumer = CallSpeedTest::testCodeHere;
        @EventHandler(eventClasses = ExampleEvent2.class)
        public final Consumer<ExampleEvent2> exampleEvent2Consumer = CallSpeedTest::testCodeHere;

        @EventHandler
        public void onEvent(ExampleEvent1 event) {
            CallSpeedTest.testCodeHere(event);
        }

        @EventHandler
        public void onEvent(ExampleEvent2 event) {
            CallSpeedTest.testCodeHere(event);
        }

    }

    public static void testCodeHere(Object event) {
        if (Math.abs(-1) == 1) {
            new HashMap<>().put("dfg", "dfgfdg");
        }
    }

}
