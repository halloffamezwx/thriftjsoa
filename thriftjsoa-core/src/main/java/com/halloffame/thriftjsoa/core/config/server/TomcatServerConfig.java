package com.halloffame.thriftjsoa.core.config.server;

import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import com.halloffame.thriftjsoa.core.server.TTomcatServer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 服务模式：Http-Tomcat
 * @author zhuwx
 */
@ToString
public class TomcatServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.HTTP_TOMCAT.getCode();

    /**
     * 嵌入式tomcat的basedir
     */
    @Getter
    @Setter
    private String basedir = TTomcatServer.TOMCAT_BASEDIR;

    /**
     * Maximum number of connections that the server accepts and processes at any
     * given time. Once the limit has been reached, the operating system may still
     * accept connections based on the "acceptCount" property.
     */
    @Getter
    @Setter
    private int maxConnections = 8192;

    /**
     * Maximum queue length for incoming connection requests when all possible request
     * processing threads are in use.
     */
    @Getter
    @Setter
    private int acceptCount = 100;

    /**
     * Maximum amount of worker threads.
     */
    @Getter
    @Setter
    private int maxThreads = 200;

    /**
     * Minimum amount of worker threads.
     */
    @Getter
    @Setter
    private int minSpareThreads = 10;

}
