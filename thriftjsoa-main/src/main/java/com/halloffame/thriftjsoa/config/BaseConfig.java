package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.constant.ProtocolType;
import com.halloffame.thriftjsoa.constant.TransportType;
import lombok.Data;
import org.apache.zookeeper.ZooKeeper;

/**
 * 基础配置
 * @author zhuwx
 */
@Data
public class BaseConfig {

    /**
     * 服务的唯一标识
     */
    private String appId;

    /**
     * 端口
     */
    private int port = CommonServer.PORT;

    /**
     * 通信是否加密
     */
	private boolean ssl = false;

    /**
     * 传输方式
     */
	private String transportType = TransportType.FASTFRAMED.getCode();

    /**
     * 传输协议
     */
	private String protocolType = ProtocolType.COMPACT.getCode();

    /**
     * 链接连通性检查的请求的不存在的接口名
     */
	private String connValidateMethodName = CommonServer.CONN_VALIDATE_METHOD_NAME;

    /**
     * 当传输方式为http时的请求path路径
     */
    private String httpPath = "";

    /**
     * 注册中心（zookeeper）连接配置
     */
    private ZkConnConfig zkConnConfig;

    /**
     * 注册中心（zookeeper）
     */
    private ZooKeeper zk;
}
