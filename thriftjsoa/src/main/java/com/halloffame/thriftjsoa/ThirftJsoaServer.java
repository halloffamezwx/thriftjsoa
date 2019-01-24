package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.ServerZkConfig;
import com.halloffame.thriftjsoa.util.JsonUtil;
import org.apache.thrift.TProcessor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * ThirftJsoa服务端
 */
public class ThirftJsoaServer {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
	
	private int port; //服务端口
	private String host; //向zk注册本服务的地址
	private ZooKeeper zk;
	private String zkConnStr; //zk连接串
	private TProcessor tProcessor; //thrift业务处理的processor
	
	private String zkRootPath = CommonServer.ZK_ROOT_PATH; //zk根路径
	private int zkSessionTimeout = CommonServer.ZK_SESSION_TIMEOUT; //zk会话的有效时间，单位是毫秒
	//服务端的一些配置，将会保存到zk节点的data上，proxy将会读取这些配置数据
	private ServerZkConfig serverZkConfig = new ServerZkConfig(); 
	
	public String getZkRootPath() {
		return zkRootPath;
	}
	public void setZkRootPath(String zkRootPath) {
		this.zkRootPath = zkRootPath;
	}
	public int getZkSessionTimeout() {
		return zkSessionTimeout;
	}
	public void setZkSessionTimeout(int zkSessionTimeout) {
		this.zkSessionTimeout = zkSessionTimeout;
	}
	public ServerZkConfig getServerZkConfig() {
		return serverZkConfig;
	}
	public void setServerZkConfig(ServerZkConfig serverZkConfig) {
		this.serverZkConfig = serverZkConfig;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	public ThirftJsoaServer(int port, String zkConnStr, TProcessor tProcessor) {
		this.tProcessor = tProcessor;
		this.zkConnStr = zkConnStr;
		this.port = port;
	}

	/**
	 * 连接zk创建节点
	 */
	private void zk(String path) throws Exception {
		//创建一个与ZooKeeper服务器的连接
		zk = new ZooKeeper(zkConnStr, zkSessionTimeout, event -> LOGGER.debug("receive event : {}", event.getType().name()));
		Stat stat = zk.exists(zkRootPath, false);
		if (stat == null) { //不存在就创建根节点
            zk.create(zkRootPath, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT); 
        }
		
		//创建一个子节点
		zk.create(path, JsonUtil.serialize(serverZkConfig).getBytes(CommonServer.ZK_NODE_CHARSET), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}

	/**
	 * 启动运行
	 */
	public void run() throws Exception {
		if (host == null || "".equals(host)) {
			InetAddress inetAddress = InetAddress.getLocalHost();
			host = inetAddress.getHostAddress(); //本地ip
		}
		String path = zkRootPath + "/" + host + CommonServer.ZK_NODE_SEPARATOR + port;
		this.zk(path);
        
		LOGGER.info("Starting the server on port {}...", port);
        CommonServer.serve(path, port, serverZkConfig.getServerConfig(), tProcessor);
	}

}
