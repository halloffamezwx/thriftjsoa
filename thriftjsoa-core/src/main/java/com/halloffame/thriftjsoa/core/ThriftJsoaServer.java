package com.halloffame.thriftjsoa.core;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.config.common.ProcessorConfig;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.config.server.ThreadedSelectorServerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * ThriftJsoa服务端
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaServer {

    /**
     * 服务模式
     */
    @Getter
    private BaseServerConfig serverConfig;

    public ThriftJsoaServer() {
        serverConfig = new ThreadedSelectorServerConfig();
    }
    public ThriftJsoaServer(BaseServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
    public ThriftJsoaServer(int port, String zkConnStr, TProcessor tProcessor) {
        serverConfig = new ThreadedSelectorServerConfig();
        serverConfig.setPort(port);

        List<ProcessorConfig> processorConfigs = new ArrayList<>();
        processorConfigs.add(new ProcessorConfig().setTProcessor(tProcessor));
        serverConfig.setProcessorConfigs(processorConfigs);

        serverConfig.setZkRegisterConfig(new ZkRegisterConfig().setZkConnStr(zkConnStr));
    }

    /**
     * 启动运行
     */
    public void run() throws Exception {
        log.info("starting the server on port {}...", serverConfig.getPort());
        CommonServer.serve(serverConfig);
    }

}
