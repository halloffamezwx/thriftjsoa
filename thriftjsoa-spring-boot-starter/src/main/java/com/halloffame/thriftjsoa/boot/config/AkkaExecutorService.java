package com.halloffame.thriftjsoa.boot.config;

import akka.actor.typed.ActorSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AkkaExecutorService implements ExecutorService {

    private final ActorSystem<Runnable> actorSystem;

    private volatile boolean isShutDown = false;

    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Wait condition to support awaitTermination
     */
    private final Condition termination = mainLock.newCondition();

    public AkkaExecutorService() {
        actorSystem = ActorSystem.create(RunnableBehavior.create(), "akkaExecutorService");
    }

    public AkkaExecutorService(ActorSystem<Runnable> actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void execute(Runnable command) {
        actorSystem.tell(command);
    }

    @Override
    public void shutdown() {
        actorSystem.terminate();
        isShutDown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return isShutDown;
    }

    @Override
    public boolean isTerminated() {
        return actorSystem.whenTerminated().isCompleted();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        //actorSystem.getWhenTerminated().thenRunAsync(() -> {
        // interrupted thread.sleep(xxx);
        //});

        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (isTerminated())
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return null;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

}
