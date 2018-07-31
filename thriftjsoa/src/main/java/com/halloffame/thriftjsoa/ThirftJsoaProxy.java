package com.halloffame.thriftjsoa;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.ServerZkConfig;
import com.halloffame.thriftjsoa.config.ThreadedSelectorServerConfig;
import com.halloffame.thriftjsoa.util.JsonUtil;

public class ThirftJsoaProxy {
	//连接池list，每个服务对应一个连接池
	private List<ConnectionPoolFactory> poolFactorys = new ArrayList<ConnectionPoolFactory>();
	//每个服务对应的配置map<host + ":" + port, serverConfig>
	//private Map<String, BaseServerConfig> serverConfigMap = new HashMap<>();
	
	private ZooKeeper zk;
	private int port; //代理服务端口
	private String zkConnStr; //zk连接串
	
	private String zkRootPath = "/thriftJsoaServer"; //zk根路径，用于取得该路径下注册的所有服务的信息
	private int zkSessionTimeout = 5000; //zk会话的有效时间，单位是毫秒
	private BaseServerConfig proxyServerConfig = new ThreadedSelectorServerConfig(); //代理服务的一些配置
	
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
	public BaseServerConfig getProxyServerConfig() {
		return proxyServerConfig;
	}
	public void setProxyServerConfig(BaseServerConfig proxyServerConfig) {
		this.proxyServerConfig = proxyServerConfig;
	}
	
	public ThirftJsoaProxy(int port, String zkConnStr) throws Exception {
		this.port = port;
		this.zkConnStr = zkConnStr;
	}
	
	public void run() throws Exception {
		this.zk();
		
		ProxyProcessor proxyProcessor = new ProxyProcessor(); //自定义的一个processor，非生成代码	
        System.out.println("Starting the proxy on port " + port + "...");
        CommonServer.serve(port, proxyServerConfig, proxyProcessor);
	}
	
	private void zk() throws Exception {
		MyWatcher myWatcher = new MyWatcher();
		zk = new ZooKeeper(zkConnStr, zkSessionTimeout, myWatcher); 
		
		List<String> servers = zk.getChildren(zkRootPath, true);
		System.out.println("zk-servers=" + servers);
		for (String server : servers) {
			addServer(server); //新增服务
		}
	}
	
	private void addServer(String server) throws Exception {
		String[] serverArr = server.split(":");
		String host = serverArr[0];
		int port = Integer.parseInt(serverArr[1]);
		
		Stat stat = new Stat();
		byte[] serverZkConfigData = zk.getData(zkRootPath + "/" + server, false, stat);
		String serverZkConfigStr = new String(serverZkConfigData, "UTF-8");
		
		ServerZkConfig serverZkConfig = JsonUtil.deserialize(serverZkConfigStr, ServerZkConfig.class);
		GenericObjectPoolConfig poolConfig = serverZkConfig.getPoolConfig();
		int socketTimeout = serverZkConfig.getSocketTimeout();
		
		BaseServerConfig serverConfig = serverZkConfig.getServerConfig();
		boolean ssl = serverConfig.isSsl();
		String transportType = serverConfig.getTransportType();
		String protocolType = serverConfig.getProtocolType();
		
        ConnectionPoolFactory poolFactory = new ConnectionPoolFactory(poolConfig, host, port, 
        		socketTimeout, ssl, transportType, protocolType);
        poolFactorys.add(poolFactory);
        //serverConfigMap.put(host + ":" + port, serverConfig);
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
        					//serverConfigMap.remove(poolFactory.toString());
        					
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
	
	//负载均衡：最小连接数（加权）
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
			TProtocol tProtocol = null; //通信协议
			
			try {
				TMessage msg = in.readMessageBegin();
				
				poolFactory = getLeastConnPoolFactory(); //取得最小连接数（加权）的服务
				tProtocol = poolFactory.getConnection(); 
				
				//BaseServerConfig serverConfig = serverConfigMap.get(poolFactory.toString());
				
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
				    x.write(out); 
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
				if (poolFactory != null && tProtocol != null) {
					poolFactory.releaseConnection(tProtocol); //归还tProtocol到连接池
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
				
				if (schemeField.type == TType.STOP) { 
					break;
				} else {
					out.writeFieldBegin(schemeField);
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
			out.writeFieldStop();
			
			in.readStructEnd();
			out.writeStructEnd();
		}	
	}

}
