package com.halloffame.thriftjsoa.sample.server;

import com.halloffame.thriftjsoa.sample.iface.UserService;
import org.apache.thrift.TProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public TProcessor tProcessor(UserService.Iface userService) {
        TProcessor tProcessor = new UserService.Processor(userService); //根据UserService.thrift生成的Processor
        /** tProcessor = new ThriftJsoaSessionProcessor<com.halloffame.thriftjsoa.sample.iface.session.UserService>(
                new com.halloffame.thriftjsoa.sample.iface.session.UserServiceImpl()); */
        return tProcessor;
    }

    /**
    @Bean
    public TjExecutorService tjExecutorService() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        TjExecutorService tjExecutorService = new TjExecutorService(executorService);
        return tjExecutorService;
    } */

}
