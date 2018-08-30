package com.halloffame.thriftjsoa.util;

import java.util.concurrent.atomic.AtomicInteger;

public class MyAtomicInteger extends AtomicInteger {
	private static final long serialVersionUID = 7326222005352583809L;

	public final int incrementAndGet(int max) {
        int current;
        int next;
        do {
            current = this.get();
            next = current >= max ? 0 : current + 1;
        } while(!this.compareAndSet(current, next));

        return next;
    }

    public final int decrementAndGet(int max) {
        int current;
        int next;
        do {
            current = this.get();
            next = current <= 0 ? max : current - 1;
        } while(!this.compareAndSet(current, next));

        return next;
    }

}
