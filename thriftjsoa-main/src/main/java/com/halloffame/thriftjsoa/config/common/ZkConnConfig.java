package com.halloffame.thriftjsoa.config.common;

import com.halloffame.thriftjsoa.common.CommonServer;
import lombok.Data;

/**
 * 注册中心（zookeeper）连接配置
 * @author zhuwx
 */
@Data
public class ZkConnConfig {

    /**
     * 根路径
     */
    private String zkRootPath = CommonServer.ZK_ROOT_PATH;

    /**
     * 节点的path
     */
    private String zkNodePath;

    /**
     * 连接串
     */
    private String zkConnStr = CommonServer.ZK_CONN_STR;

    /**
     * 会话的有效时间，单位是毫秒
     */
    private int zkSessionTimeout = CommonServer.ZK_SESSION_TIMEOUT;

}
