package org.lff;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Feifei Liu
 * @datetime Aug 07 2017 10:29
 */
public class NamedThreadFactory implements ThreadFactory {

    private AtomicInteger count = new AtomicInteger(0);

    private final String name;

    public NamedThreadFactory(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "<UNAMED>";
        }
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName(name + "-" + count.incrementAndGet());
        return t;
    }
}

