package com.halloffame.thriftjsoa.core.config.register;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.config.BaseRegisterConfig;
import com.halloffame.thriftjsoa.core.constant.RegisterType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 注册中心（zookeeper）配置
 * @author zhuwx
 */
@ToString
@Accessors(chain = true)
public class ZkRegisterConfig extends BaseRegisterConfig {

    /**
     * 注册中心类型
     */
    @Getter
    private final String registerType = RegisterType.ZOOKEEPER.getCode();

    /**
     * 根路径
     */
    @Getter
    @Setter
    private String zkRootPath = CommonServer.ZK_ROOT_PATH;

    /**
     * 节点的path
     */
    @Getter
    @Setter
    private String zkNodePath;

    /**
     * 连接串
     */
    @Getter
    @Setter
    private String zkConnStr; // = CommonServer.ZK_CONN_STR;

    /**
     * 会话的有效时间，单位是毫秒
     */
    @Getter
    @Setter
    private int zkSessionTimeout = CommonServer.ZK_SESSION_TIMEOUT;

}
