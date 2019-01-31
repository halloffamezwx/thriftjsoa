package com.halloffame.thriftjsoa.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 原子计数器，从0到max循环计数
 */
public class MyAtomicInteger extends AtomicInteger {
	private static final long serialVersionUID = 7326222005352583809L;

	public MyAtomicInteger(int initialValue) {
	    super(initialValue);
    }

    /**
     * 累加一
     */
	public final int incrementAndGet(int max) {
        int current;
        int next;
        do {
            current = this.get();
            next = current >= max ? 0 : current + 1;
        } while(!this.compareAndSet(current, next));

        return next;
    }

    /**
     * 累减一
     */
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
