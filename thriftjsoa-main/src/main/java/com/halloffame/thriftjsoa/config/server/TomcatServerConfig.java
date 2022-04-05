package com.halloffame.thriftjsoa.config.server;

import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.constant.ServerType;
import com.halloffame.thriftjsoa.server.TTomcatServer;
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

}
