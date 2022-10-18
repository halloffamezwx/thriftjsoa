package com.halloffame.thriftjsoa.boot.proxy.test;

import com.halloffame.thriftjsoa.boot.config.TjProxyExecutorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Executors;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    //@Bean
    public TjProxyExecutorService tjProxyExecutorService() {
        return new TjProxyExecutorService(Executors.newFixedThreadPool(5));
    }
}
