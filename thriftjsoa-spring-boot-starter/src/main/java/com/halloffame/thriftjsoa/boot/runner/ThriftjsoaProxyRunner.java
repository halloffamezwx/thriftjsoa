package com.halloffame.thriftjsoa.boot.runner;

import com.halloffame.thriftjsoa.ThriftJsoaProxy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 代理Runner
 * @author zhuwx
 */
public class ThriftjsoaProxyRunner implements ApplicationRunner {

    private ThriftJsoaProxy thirftJsoaProxy;

    public ThriftjsoaProxyRunner(ThriftJsoaProxy thirftJsoaProxy) {
        this.thirftJsoaProxy = thirftJsoaProxy;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        thirftJsoaProxy.run();
    }

}
