package com.halloffame.thriftjsoa;

import org.apache.thrift.TProcessor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.ServerZkConfig;
import com.halloffame.thriftjsoa.util.JsonUtil;

public class ThirftJsoaServer {
	private int port; //服务端口
	private String host; //向zk注册本服务的地址
	private ZooKeeper zk;
	private String zkConnStr; //zk连接串
	private TProcessor tProcessor; //thrift业务处理的processor
	
	private String zkRootPath = "/thriftJsoaServer"; //zk根路径
	private int zkSessionTimeout = 5000; //zk会话的有效时间，单位是毫秒
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
	
	public ThirftJsoaServer(int port, String zkConnStr, String host, TProcessor tProcessor) throws Exception {
		this.tProcessor = tProcessor;
		this.host = host;
		this.zkConnStr = zkConnStr;
		this.port = port;
	}
	
	private void zk() throws Exception {
		//创建一个与ZooKeeper服务器的连接
		zk = new ZooKeeper(zkConnStr, zkSessionTimeout, null); 
		Stat stat = zk.exists(zkRootPath, false);
		if (stat == null) { //不存在就创建根节点
            zk.create(zkRootPath, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT); 
        }
		
		//创建一个子节点
		zk.create(zkRootPath + "/" + host + ":" + port, 
				JsonUtil.serialize(serverZkConfig).getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	} 
	
	public void run() throws Exception {
		this.zk();
        
        System.out.println("Starting the server on port " + port + "...");
        CommonServer.serve(port, serverZkConfig.getServerConfig(), tProcessor);
	}

}
