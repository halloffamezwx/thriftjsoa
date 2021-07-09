package com.halloffame.thriftjsoa.sample.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
    @Bean
    public TjExecutorService tjExecutorService() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        TjExecutorService tjExecutorService = new TjExecutorService(executorService);
        return tjExecutorService;
    } */
}
