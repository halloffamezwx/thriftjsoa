package com.halloffame.thriftjsoa.boot.server.test;

import com.halloffame.thriftjsoa.boot.annotation.EnableTjClients;
import com.halloffame.thriftjsoa.boot.annotation.EnableTjSessionManagement;
import com.halloffame.thriftjsoa.boot.config.TjExecutorService;
import com.halloffame.thriftjsoa.core.config.common.ProcessorConfig;
import com.halloffame.thriftjsoa.test.UserService;
import org.apache.thrift.TProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableTjClients
//@TjClientScan("com.xxx.xxx")
@EnableTjSessionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public List<ProcessorConfig> processorConfigs(UserService.Iface userService) {
        List<ProcessorConfig> processorConfigs = new ArrayList<>();
        TProcessor tProcessor = new UserService.Processor(userService); //根据UserService.thrift生成的Processor
        processorConfigs.add(new ProcessorConfig().setTProcessor(tProcessor));
        /** tProcessor = new ThriftJsoaSessionProcessor<com.halloffame.thriftjsoa.test.session.UserService>(
                new com.halloffame.thriftjsoa.test.session.UserServiceImpl()); */
        return processorConfigs;
    }

    //@Bean
    public TjExecutorService tjExecutorService() {
        //new TjExecutorService(new AkkaExecutorService());
        return new TjExecutorService(Executors.newFixedThreadPool(5));
    }

}
