package com.halloffame.thriftjsoa;

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

public class ThirftJsoaServer {
	private String ip = "localhost";
	private int port = 9090;
	private ZooKeeper zk;
	private String zkRootPath = "/thriftJsoaServer";
	private TProcessor tProcessor = null;
	
	public ThirftJsoaServer(int port, String zkConnStr, String ip, TProcessor tProcessor) throws Exception {
		this.port = port;
		this.ip = ip;
		this.tProcessor = tProcessor;
		this.zk(zkConnStr);
	}
	
	private void zk(String zkConnStr) throws Exception {
		//创建一个与ZooKeeper服务器的连接
		zk = new ZooKeeper(zkConnStr, 5000, null); 
		Stat stat = zk.exists(zkRootPath, false);
		if (stat == null) { //不存在就创建根节点
            zk.create(zkRootPath, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT); 
        }
		//创建一个子节点
		zk.create(zkRootPath + "/" + ip + ":" + port, "someData".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}
	
	public void run() throws Exception {
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
