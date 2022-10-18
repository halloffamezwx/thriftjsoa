package com.halloffame.thriftjsoa.core.config.server;

import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import lombok.Getter;
import lombok.ToString;

/**
 * 服务模式：单条线程非阻塞io
 * @author zhuwx
 */
@ToString
public class NonblockingServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.NONBLOCKING.getCode();

}
