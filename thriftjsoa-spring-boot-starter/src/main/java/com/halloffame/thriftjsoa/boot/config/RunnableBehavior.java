package com.halloffame.thriftjsoa.boot.config;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class RunnableBehavior extends AbstractBehavior<Runnable> {

    public static Behavior<Runnable> create() {
        return Behaviors.setup(RunnableBehavior::new);
    }

    private RunnableBehavior(ActorContext<Runnable> context) {
        super(context);
    }

    @Override
    public Receive<Runnable> createReceive() {
        return newReceiveBuilder().onMessage(Runnable.class, this::run).build();
    }

    private Behavior<Runnable> run(Runnable command) {
        command.run();
        return this;
    }

}
