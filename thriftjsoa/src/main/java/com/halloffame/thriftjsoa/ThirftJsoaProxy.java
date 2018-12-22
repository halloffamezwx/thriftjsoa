package com.halloffame.thriftjsoa;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.ServerZkConfig;
import com.halloffame.thriftjsoa.config.ThreadedSelectorServerConfig;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.WeightRandomLoadBalance;
import com.halloffame.thriftjsoa.util.JsonUtil;

public class ThirftJsoaProxy {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
	
	private ZooKeeper zk;
	private int port; //代理服务端口
	private String zkConnStr; //zk连接串
	
	private String zkRootPath = "/thriftJsoaServer"; //zk根路径，用于取得该路径下注册的所有服务的信息
	private int zkSessionTimeout = 5000; //zk会话的有效时间，单位是毫秒
	private BaseServerConfig proxyServerConfig = new ThreadedSelectorServerConfig(); //代理服务的一些配置
	
	private LoadBalanceAbstract loadBalance = new WeightRandomLoadBalance(); //负载均衡，默认随机（加权）
	
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
	public LoadBalanceAbstract getLoadBalance() {
		return loadBalance;
	}
	public void setLoadBalance(LoadBalanceAbstract loadBalance) {
		this.loadBalance = loadBalance;
	}
	
	public ThirftJsoaProxy(int port, String zkConnStr) throws Exception {
		this.port = port;
		this.zkConnStr = zkConnStr;
	}
	
	public void run() throws Exception {
		this.zk();
		
		ProxyProcessor proxyProcessor = new ProxyProcessor(); //自定义的一个processor，非生成代码	
		LOGGER.info("Starting the proxy on port {}...", port);
        CommonServer.serve(port, proxyServerConfig, proxyProcessor);
	}
	
	private void zk() throws Exception {
		MyWatcher myWatcher = new MyWatcher();
		zk = new ZooKeeper(zkConnStr, zkSessionTimeout, myWatcher); 
		
		List<String> servers = zk.getChildren(zkRootPath, true);
		LOGGER.info("zk-servers={}", servers);
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
        
        loadBalance.addPoolFactory(poolFactory);
	}
	
	class MyWatcher implements Watcher {
		@Override
		public void process(WatchedEvent event) {
			LOGGER.info("已经触发了{}事件！", event.getType()); 
			//子节点变动：新增或删除
        	if (event.getType() == EventType.NodeChildrenChanged) {
        		try {
        			List<String> servers = zk.getChildren(zkRootPath, true);
        			List<ConnectionPoolFactory> poolFactorys = loadBalance.getPoolFactorys();
        			
        			for (String server : servers) {
        				boolean isFind = false;
        				for (ConnectionPoolFactory poolFactory : poolFactorys) {
        					if ( server.equals(poolFactory.toString()) ) {
        						isFind = true;
        						break;
        					}
        				}
        				if (!isFind) {
        					LOGGER.info("上线{}", server);
        					addServer(server); //新增服务
        				}
        			}
        			
        			Iterator<ConnectionPoolFactory> it = poolFactorys.iterator();
        			while (it.hasNext()) {
        				ConnectionPoolFactory poolFactory = it.next();
        				boolean isFind = false;
        				
        				for (String server : servers) {
        					if ( poolFactory.toString().equals(server) ) {
        						isFind = true;
        						break;
        					}
        				}
        				
        				if (!isFind) { //下线服务
        					LOGGER.info("下线{}", poolFactory);
        					loadBalance.removePoolFactory(poolFactory, it); 
        				}
        			}
        			
				} catch (Exception e) {
					LOGGER.error("zk NodeChildrenChanged process exception:", e); 
				} 
        	} 
		}
		
	}
	
	/**
	 * 自定义Processor
	 */
	public class ProxyProcessor implements TProcessor {
		@Override
		public boolean process(TProtocol in, TProtocol out) throws TException {
			ConnectionPoolFactory poolFactory = null;
			TProtocol tProtocol = null; //通信协议
			
			try {
				TMessage msg = in.readMessageBegin();
				
				LoadBalanceBean loadBalanceBean = loadBalance.getLoadBalanceConnPool(); 
				poolFactory = loadBalanceBean.getConnectionPoolFactory();
				tProtocol = loadBalanceBean.getProtocol();
				
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
		
		/**
		 * 读写数据
		 */
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
				
				readWriteField(schemeField.type, in, out);
				
				in.readFieldEnd();
				out.writeFieldEnd();
			}
			out.writeFieldStop();
			
			in.readStructEnd();
			out.writeStructEnd();
		}
		
		/**
		 * 读写字段数据
		 */
		private void readWriteField(byte fieldtype, TProtocol in, TProtocol out) throws TException {
			switch (fieldtype) {
				case TType.VOID:
					TProtocolUtil.skip(in, fieldtype);
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
					 * readMapBegin返回的TMap对象有3个字段keyType，valueType，size，
					 * 就是map的key的类型，value的类型，map的大小，
					 * 从0到size循环按类型读取key和value就行了。
					 */
					TMap tMap = in.readMapBegin();
					out.writeMapBegin(tMap);
					for (int i = 0; i < tMap.size; i++) {
						readWriteField(tMap.keyType, in, out);
						readWriteField(tMap.valueType, in, out);
					}
					in.readMapEnd();
					out.writeMapEnd();
					break;
				case TType.SET:
					TSet tSet = in.readSetBegin();
					out.writeSetBegin(tSet);
					for (int i = 0; i < tSet.size; i++) {
						readWriteField(tSet.elemType, in, out);
					}
					in.readSetEnd();
					out.writeSetEnd();
					break;
				case TType.LIST:
					TList tList = in.readListBegin();
					out.writeListBegin(tList);
					for (int i = 0; i < tList.size; i++) {
						readWriteField(tList.elemType, in, out);
					}
					in.readListEnd();
					out.writeListEnd();
					break;
				case TType.ENUM:
					//Enum类型传输时是个i32
					TProtocolUtil.skip(in, fieldtype);
					break;
				default:
					TProtocolUtil.skip(in, fieldtype);
			}
		}
		
	}

}
