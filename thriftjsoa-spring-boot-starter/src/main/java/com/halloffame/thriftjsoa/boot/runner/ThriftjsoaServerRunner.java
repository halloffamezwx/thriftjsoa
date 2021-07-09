package com.halloffame.thriftjsoa.boot.runner;

import com.halloffame.thriftjsoa.ThriftJsoaServer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 服务Runner
 * @author zhuwx
 */
public class ThriftjsoaServerRunner implements ApplicationRunner {

    private ThriftJsoaServer thirftJsoaServer;

    public ThriftjsoaServerRunner(ThriftJsoaServer thirftJsoaServer) {
        this.thirftJsoaServer = thirftJsoaServer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        thirftJsoaServer.run();
    }

}
