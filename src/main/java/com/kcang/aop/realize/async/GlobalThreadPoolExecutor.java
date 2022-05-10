package com.kcang.aop.realize.async;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class GlobalThreadPoolExecutor {
    /**
     * The default thread factory
     */
    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = name+"-" +
                    poolNumber.getAndIncrement() +
                    "-pool-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    static ThreadPoolExecutor ArrayBlockingPool = new ThreadPoolExecutor(
            3, 18, 180, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3), new DefaultThreadFactory("ArrayBlocking")
    );
    static ThreadPoolExecutor SynchronousPool = new ThreadPoolExecutor(
            1, 18, 180, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new DefaultThreadFactory("Synchronous")
    );
}
