package com.halloffame.thriftjsoa.boot.runner;

import com.halloffame.thriftjsoa.core.ThriftJsoaProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 代理Runner
 * @author zhuwx
 */
@Slf4j
public class ThriftjsoaProxyRunner implements ApplicationRunner {

    private ThriftJsoaProxy thirftJsoaProxy;

    public ThriftjsoaProxyRunner(ThriftJsoaProxy thirftJsoaProxy) {
        this.thirftJsoaProxy = thirftJsoaProxy;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(() -> {
            try {
                thirftJsoaProxy.run();
            } catch (Exception e) {
                log.error("代理端启动异常：", e);
            }
        }).start();
    }

}
