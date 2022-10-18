package com.halloffame.thriftjsoa.core.config.server;

import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import lombok.Getter;
import lombok.ToString;

/**
 * 服务模式：单线程阻塞io
 * @author zhuwx
 */
@ToString
public class SimpleServerConfig extends BaseServerConfig {

    /**
     * 服务模式
     */
    @Getter
    private String serverType = ServerType.SIMPLE.getCode();

}
