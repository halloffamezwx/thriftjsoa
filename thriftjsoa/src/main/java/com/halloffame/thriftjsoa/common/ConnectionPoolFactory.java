package com.halloffame.thriftjsoa.common;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接池工厂类
 */
public class ConnectionPoolFactory {  
	private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
    private GenericObjectPool<TProtocol> pool; //连接池
    private String hostStr; //主机 + 端口
  
    public ConnectionPoolFactory(GenericObjectPoolConfig config, String host, int port, 
    		int socketTimeout, boolean ssl, String transportType, String protocolType) {  
    	hostStr = host + CommonServer.ZK_NODE_SEPARATOR + port;
    	ConnectionFactory objFactory = new ConnectionFactory(host, port, socketTimeout, ssl, transportType, protocolType);  
        pool = new GenericObjectPool<>(objFactory, config);
    }
    
    @Override
    public String toString() {
    	return hostStr;
    }
    
    public void close() {
    	pool.close();
    }

	/**
	 * 取得最大连接数
	 */
    public int getMaxTotal() {
    	return pool.getMaxTotal();
    }

	/**
	 * 取得活动连接数
	 */
    public int getNumActive() {
    	return pool.getNumActive();
    }

	/**
	 * 取得权重值
	 */
    public double getWeight() {
    	//活动的连接数 除以 最大连接数
    	double numActive = pool.getNumActive(); 
    	double maxTotal = pool.getMaxTotal();
    	LOGGER.info("{} getWeight={} / {}", hostStr, numActive, maxTotal); 
    	return numActive / maxTotal;
    }

	/**
	 * 从池里获取一个TProtocol对象
	 */
    public TProtocol getConnection() {
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 把一个TProtocol对象归还到池里
	 */
    public void releaseConnection(TProtocol tProtocol) {  
        pool.returnObject(tProtocol);   
    }  
    
    /**
     * 连接池管理的对象TProtocol的工厂类，
     * GenericObjectPool会使用此类的create方法来
     * 创建TProtocol对象并放进pool里进行管理等操作。
     */
    class ConnectionFactory extends BasePooledObjectFactory<TProtocol> {
		public static final int seqid = 0; //检查对象的有效性请求的seqid
		public static final String methodName = "proxyValidate"; //检查对象的有效性请求的不存在的接口名
		public static final String exceptionMsg = "Invalid method name: '" + methodName + "'"; //检查对象的有效性期待服务端返回的错误消息

    	private String host;  
        private int port;
        private int socketTimeout;
        private String transportType; 
        private String protocolType;
        private boolean ssl;
          
        public ConnectionFactory(String host, int port, int socketTimeout, boolean ssl, String transportType, String protocolType) {  
        	this.host = host;
        	this.port = port;
        	this.socketTimeout = socketTimeout;
        	this.ssl = ssl;
        	this.transportType = transportType;
        	this.protocolType = protocolType;
        }  

		/**
		 * 创建TProtocol类型对象方法
		 */
        @Override
		public TProtocol create() throws Exception {
        	TTransport transport; //通信方式
        	TSocket socket;
	        if (ssl == true) {
	            socket = TSSLTransportFactory.getClientSocket(host, port, 0);
	        } else {
	            socket = new TSocket(host, port);
	        }
	        socket.setTimeout(socketTimeout);
	        
	        transport = socket;
	        if (transportType.equals(TransportType.BUFFERED)) {
	        } else if (transportType.equals(TransportType.FRAMED)) {
	            transport = new TFramedTransport(transport);
	        } else if (transportType.equals(TransportType.FASTFRAMED)) {
	            transport = new TFastFramedTransport(transport);
	        }
	        
	        if ( !transport.isOpen() ) {
	        	transport.open();
	        }
	        
	        TProtocol tProtocol; //通信协议
		    if (protocolType.equals(ProtocolType.JSON)) {
		        tProtocol = new TJSONProtocol(transport);
		    } else if (protocolType.equals(ProtocolType.COMPACT)) {
		        tProtocol = new TCompactProtocol(transport);
		    } else {
		        tProtocol = new TBinaryProtocol(transport);
		    }
		    
			return tProtocol;
		}

		/**
		 * 把TProtocol对象打包成池管理的对象PooledObject<TProtocol>
		 */
		@Override
		public PooledObject<TProtocol> wrap(TProtocol tProtocol) {
			return new DefaultPooledObject<>(tProtocol);
		}

		/**
		 * 销毁对象
		 */
		@Override
	    public void destroyObject(final PooledObject<TProtocol> p) throws Exception { 
			TTransport tTransport = p.getObject().getTransport();
			tTransport.flush();
			tTransport.close();
	    }

		/**
		 * 检查对象的有效性
		 * 请求一个不存在的接口，服务端会返回相应的错误信息，这样就可以判断此链路是否相通
		 */
		@Override
	    public boolean validateObject(final PooledObject<TProtocol> p) {
			TProtocol tProtocol = p.getObject();
			if ( tProtocol.getTransport().isOpen() ) {
				try {
					tProtocol.writeMessageBegin(new TMessage(methodName, TMessageType.CALL, seqid));
					tProtocol.writeStructBegin(new TStruct(""));
					tProtocol.writeFieldStop();
					tProtocol.writeStructEnd();
					tProtocol.writeMessageEnd();
					tProtocol.getTransport().flush();
					
					TMessage msg = tProtocol.readMessageBegin();
					if (msg.type == TMessageType.EXCEPTION) {
						TApplicationException x = TApplicationException.read(tProtocol);
					    tProtocol.readMessageEnd();
					    if (x.getType() == TApplicationException.UNKNOWN_METHOD && exceptionMsg.equals(x.getMessage())) { 
					    	return true;
					    } else {
					    	throw x;
						}
					}
				} catch (TException e) {
					LOGGER.info("validateObject TException:", e);
				}
			}
			
	        return false;
	    }
		
		//borrowObject时触发
		//@Override
	    //public void activateObject(final PooledObject<TProtocol> p) throws Exception { }
		//returnObject时触发
		//@Override
	    //public void passivateObject(final PooledObject<TProtocol> p) throws Exception { }
    }
    
} 
