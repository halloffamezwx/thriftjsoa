package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.config.server.ThreadedSelectorServerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;

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
        serverConfig.setProcessor(tProcessor);

        ZkConnConfig zkConnConfig = new ZkConnConfig();
        zkConnConfig.setZkConnStr(zkConnStr);
        serverConfig.setZkConnConfig(zkConnConfig);
    }

    /**
     * 启动运行
     */
    public void run() throws Exception {
        log.info("starting the server on port {}...", serverConfig.getPort());
        CommonServer.serve(serverConfig);
    }

}
