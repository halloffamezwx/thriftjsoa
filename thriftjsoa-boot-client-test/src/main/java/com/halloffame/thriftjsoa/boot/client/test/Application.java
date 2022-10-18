package com.halloffame.thriftjsoa.boot.client.test;

import com.halloffame.thriftjsoa.boot.annotation.EnableTjSessionManagement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableTjSessionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
