package com.halloffame.thriftjsoa.boot.runner;

import com.halloffame.thriftjsoa.ThriftJsoaServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 服务Runner
 * @author zhuwx
 */
@Slf4j
public class ThriftjsoaServerRunner implements ApplicationRunner {

    private ThriftJsoaServer thirftJsoaServer;

    public ThriftjsoaServerRunner(ThriftJsoaServer thirftJsoaServer) {
        this.thirftJsoaServer = thirftJsoaServer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(() -> {
            try {
                thirftJsoaServer.run();
            } catch (Exception e) {
                log.error("服务端启动异常：", e);
            }
        }).start();
    }

}
