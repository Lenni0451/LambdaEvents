package net.lenni0451.le;

public class Triple<A, B, C> {

    private final A a;
    private final B b;
    private final C c;

    public Triple(final A a, final B b, final C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public A getA() {
        return this.a;
    }

    public B getB() {
        return this.b;
    }

    public C getC() {
        return this.c;
    }

}
