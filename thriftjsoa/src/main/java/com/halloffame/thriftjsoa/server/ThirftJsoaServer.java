package com.halloffame.thriftjsoa.server;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.halloffame.thriftjsoa.util.JsonUtil;

public class ThirftJsoaServer {
	private int port;
	private String host;
	private ZooKeeper zk;
	private String zkConnStr;
	private TProcessor tProcessor;
	
	private String zkRootPath = "/thriftJsoaServer";
	private int zkSessionTimeout = 5000;
	private ServerConfig serverConfig = new ServerConfig();
	
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
	public ServerConfig getServerConfig() {
		return serverConfig;
	}
	public void setServerConfig(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
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
		zk.create(zkRootPath + "/" + host + ":" + port, JsonUtil.serialize(serverConfig).getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	} 
	
	public void run() throws Exception {
		this.zk();
		
        TProtocolFactory tProtocolFactory = new TCompactProtocol.Factory(); //通信协议
        TTransportFactory tTransportFactory = new TFastFramedTransport.Factory(); //通信方式

    	TNonblockingServerSocket tNonblockingServerSocket =
    			new TNonblockingServerSocket(new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs().port(port));
    	
    	TThreadedSelectorServer.Args tThreadedSelectorServerArgs
          	= new TThreadedSelectorServer.Args(tNonblockingServerSocket);
    	tThreadedSelectorServerArgs.processor(tProcessor);
    	tThreadedSelectorServerArgs.protocolFactory(tProtocolFactory);
    	tThreadedSelectorServerArgs.transportFactory(tTransportFactory);
    		
    	TServer serverEngine = new TThreadedSelectorServer(tThreadedSelectorServerArgs); //服务器模式
        
        System.out.println("Starting the server on port " + port + "...");
        serverEngine.serve();
	}

}
