package com.halloffame.thriftjsoa.core.config;

import com.halloffame.thriftjsoa.core.common.CommonClient;
import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.constant.ProtocolType;
import com.halloffame.thriftjsoa.core.constant.TransportType;
import lombok.Data;
import org.apache.curator.framework.CuratorFramework;

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
     * 主机
     */
    private String host; // = "localhost";

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
    private String generalTransportType = TransportType.FASTFRAMED.getCode();
	private String inTransportType = TransportType.FASTFRAMED.getCode();
    private String outTransportType = TransportType.FASTFRAMED.getCode();

    /**
     * 传输协议
     */
    private String generalProtocolType = ProtocolType.COMPACT.getCode();
	private String inProtocolType = ProtocolType.COMPACT.getCode();
    private String outProtocolType = ProtocolType.COMPACT.getCode();

    /**
     * 链接连通性检查的请求的不存在的接口名
     */
	private String connValidateMethodName = CommonServer.CONN_VALIDATE_METHOD_NAME;

    /**
     * 优雅关机的请求的不存在的接口名
     */
    private String shutdownGracefulMethodName = CommonServer.SHUTDOWN_GRACEFUL_METHOD_NAME;

    /**
     * 获取服务状态的请求的不存在的接口名
     */
    private String getServerStatusMethodName = CommonServer.GET_SERVER_STATUS_METHOD_NAME;

    /**
     * 当传输方式为http或服务模式为tomcat时的请求path路径
     */
    private String httpPath = "";

    /**
     * 注册中心配置
     */
    private ZkRegisterConfig zkRegisterConfig;

    /**
     * 注册中心（zookeeper）
     */
    //private ZooKeeper zk;

    /**
     * 注册中心（zookeeper）- CuratorFramework
     */
    private CuratorFramework zkCf;

    /**
     * socket读超时时间，设为0表示不超时
     */
    private int socketTimeOut = CommonClient.SOCKET_TIME_OUT;
}
