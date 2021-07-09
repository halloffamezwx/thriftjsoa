package com.halloffame.thriftjsoa.sample.client;

import com.halloffame.thriftjsoa.boot.annotation.EnableThriftjsoaSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableThriftjsoaSession
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
