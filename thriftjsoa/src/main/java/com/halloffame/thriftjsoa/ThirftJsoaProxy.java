package com.halloffame.thriftjsoa;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;

public class ThirftJsoaProxy {
	private List<ConnectionPoolFactory> poolFactorys = new ArrayList<ConnectionPoolFactory>();
	private ZooKeeper zk;
	private int port = 4567;
	private String zkRootPath = "/thriftJsoaServer";
	
	public ThirftJsoaProxy(int port, String zkConnStr) throws Exception {
		this.port = port;
		this.zk(zkConnStr);
	}
	
	public void run() throws Exception {
		ProxyProcessor proxyProcessor = new ProxyProcessor(); //自定义的一个processor，非生成代码	
        TProtocolFactory tProtocolFactory = new TCompactProtocol.Factory();  //通信协议
        TTransportFactory tTransportFactory = new TFastFramedTransport.Factory(); //通信方式

    	TNonblockingServerSocket tNonblockingServerSocket =
    			new TNonblockingServerSocket(new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs().port(port));
    	
    	TThreadedSelectorServer.Args tThreadedSelectorServerArgs
          	= new TThreadedSelectorServer.Args(tNonblockingServerSocket);
    	tThreadedSelectorServerArgs.processor(proxyProcessor);
    	tThreadedSelectorServerArgs.protocolFactory(tProtocolFactory);
    	tThreadedSelectorServerArgs.transportFactory(tTransportFactory);
    		
    	TServer serverEngine = new TThreadedSelectorServer(tThreadedSelectorServerArgs); //服务器模式
        System.out.println("Starting the proxy on port " + port + "...");
        serverEngine.serve();
	}
	
	private void zk(String zkConnStr) throws Exception {
		if ( zkConnStr == null || "".equals(zkConnStr.trim()) ) {
			zkConnStr = "localhost:2181";
		}
		MyWatcher myWatcher = new MyWatcher();
		zk = new ZooKeeper(zkConnStr, 5000, myWatcher); 
		
		List<String> servers = zk.getChildren(zkRootPath, true);
		System.out.println("zk-servers=" + servers);
		for (String server : servers) {
			addServer(server);
		}
	}
	
	private void addServer(String server) {
		String[] serverArr = server.split(":");
		String ip = serverArr[0];
		int port = Integer.parseInt(serverArr[1]);
		
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	    //链接池中最大连接数，默认为8
        config.setMaxTotal(30); 
        //当连接池资源耗尽时，调用者最大阻塞的时间，超时将跑出异常。单位：毫秒；默认为-1，表示永不超时
        config.setMaxWaitMillis(3000);
        
        ConnectionPoolFactory poolFactory = new ConnectionPoolFactory(config, ip, port); 
        poolFactorys.add(poolFactory);
	}
	
	class MyWatcher implements Watcher {
		@Override
		public void process(WatchedEvent event) {
			System.out.println("已经触发了" + event.getType() + "事件！"); 
			//子节点变动：新增或删除
        	if (event.getType() == EventType.NodeChildrenChanged) {
        		try {
        			List<String> servers = zk.getChildren(zkRootPath, true);
        			for (String server : servers) {
        				boolean isFind = false;
        				for (ConnectionPoolFactory poolFactory : poolFactorys) {
        					if ( server.equals(poolFactory.toString()) ) {
        						isFind = true;
        						break;
        					}
        				}
        				if (!isFind) {
        					System.out.println("上线" + server);
        					addServer(server); //新增服务
        				}
        			}
        			
        			for (int i = 0; i < poolFactorys.size(); i++) {
        				ConnectionPoolFactory poolFactory = poolFactorys.get(i);
        				boolean isFind = false;
        				for (String server : servers) {
        					if ( poolFactory.toString().equals(server) ) {
        						isFind = true;
        						break;
        					}
        				}
        				
        				if (!isFind) { //下线服务
        					System.out.println("下线" + poolFactory);
        					poolFactory.close();
        					poolFactorys.remove(i);
        					poolFactory = null;
        				}
        			}
				} catch (Exception e) {
					e.printStackTrace();
				} 
        	} 
		}
		
	}
	
	//最小连接数（加权）
	private ConnectionPoolFactory getLeastConnPoolFactory() {
		ConnectionPoolFactory selectPoolFactory = poolFactorys.get(0);
		double selectWeight = selectPoolFactory.getWeight();
		System.out.println("poolFactorys=" + poolFactorys); 
		
		for (int i = 1; i < poolFactorys.size(); i++) {
			ConnectionPoolFactory poolFactory = poolFactorys.get(i);
			double weight = poolFactory.getWeight();
			System.out.println(selectWeight + "--" + weight);
			
			//这里为了简单，没有实现：如果有多个后端的conns/weight值同为最小的，那么对它们采用加权轮询算法
			if ( weight < selectWeight ) {
				selectPoolFactory = poolFactory;
				selectWeight = weight;
			}
		}
		
		return selectPoolFactory;
	}
	
	class ProxyProcessor implements TProcessor {
		@Override
		public boolean process(TProtocol in, TProtocol out) throws TException {
			ConnectionPoolFactory poolFactory = null;
			TTransport transport = null;
			
			try {
				poolFactory = getLeastConnPoolFactory(); //取得最小连接数（加权）的服务
				transport = poolFactory.getConnection(); 
				TProtocol tProtocol = new TCompactProtocol(transport);
				
				TMessage msg = in.readMessageBegin();
				tProtocol.writeMessageBegin(msg);

				readWriteData(in, tProtocol);
				
				in.readMessageEnd();
				tProtocol.writeMessageEnd();
				tProtocol.getTransport().flush();
				
				int seqid = msg.seqid;
				String methodName = msg.name;
				
				msg = tProtocol.readMessageBegin();
				out.writeMessageBegin(msg);
				
				if (msg.type == TMessageType.EXCEPTION) {
				    TApplicationException x = TApplicationException.read(tProtocol);
				    tProtocol.readMessageEnd();
				    out.writeMessageEnd();
				    throw x;
				}
				if (msg.seqid != seqid) {
				    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, methodName + " failed: out of sequence response");
				}
				
				readWriteData(tProtocol, out);
				
				tProtocol.readMessageEnd();
				out.writeMessageEnd();
				out.getTransport().flush();

				return true;
			} finally {
				if (poolFactory != null && transport != null) {
					poolFactory.releaseConnection(transport); //归还transport到连接池
				}
			}
		}
		
		//读写字段数据
		private void readWriteData(TProtocol in, TProtocol out) throws TException {
			in.readStructBegin();
			out.writeStructBegin(new TStruct(""));
			
			TField schemeField;
			while (true) {
				schemeField = in.readFieldBegin();
				out.writeFieldBegin(schemeField);
				if (schemeField.type == TType.STOP) { 
					out.writeFieldStop();
					break;
				}
				
				switch (schemeField.type) {
					case TType.VOID:
						TProtocolUtil.skip(in, schemeField.type);
						break;
					case TType.BOOL:
						out.writeBool(in.readBool());
						break;
					case TType.BYTE:
						out.writeByte(in.readByte());
						break;
					case TType.DOUBLE:
						out.writeDouble(in.readDouble());
						break;
					case TType.I16:
						out.writeI16(in.readI16());
						break;
					case TType.I32:
						out.writeI32(in.readI32()); 
						break;
					case TType.I64:
						out.writeI64(in.readI64());
						break;
					case TType.STRING:
						out.writeString(in.readString());
						break;
					case TType.STRUCT:
						readWriteData(in, out);
						break;
					case TType.MAP:
						/**
						 * 这里我懒了，不想写了，readMapBegin返回的TMap对象有3个字段
						 * keyType，valueType，size，没错就是map的key的类型，value的类型，map的大小
						 * 从0到size累计循环的按类型读取key和读取value，构造一个hashmap就可以了
						 */
						//out.writeMapBegin(in.readMapBegin());
						//in.readMapEnd();
						//out.writeMapEnd();
						break;
					case TType.SET:
						//同理MAP类型
						//out.writeSetBegin(in.readSetBegin());
						//in.readSetEnd();
						//out.writeSetEnd();
						break;
					case TType.LIST:
						//同理MAP类型
						//out.writeListBegin(in.readListBegin());
						//in.readListEnd();
						//out.writeListEnd();
						break;
					case TType.ENUM:
						//Enum类型传输时是个i32
						TProtocolUtil.skip(in, schemeField.type);
						break;
					default:
						TProtocolUtil.skip(in, schemeField.type);
				}
				
				in.readFieldEnd();
				out.writeFieldEnd();
			}
			in.readStructEnd();
			out.writeStructEnd();
		}	
	}

}
