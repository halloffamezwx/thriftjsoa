package com.halloffame.thriftjsoa.common;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TApplicationException;
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
  
public class ConnectionPoolFactory {  
    private GenericObjectPool<TProtocol> pool;  
    private String hostStr;
  
    public ConnectionPoolFactory(GenericObjectPoolConfig config, String host, int port, 
    		int socketTimeout, boolean ssl, String transportType, String protocolType) {  
    	hostStr = host + ":" + port;
    	ConnectionFactory objFactory = new ConnectionFactory(host, port, socketTimeout, ssl, transportType, protocolType);  
        pool = new GenericObjectPool<TProtocol>(objFactory, config);  
    }
    
    @Override
    public String toString() {
    	return hostStr;
    }
    
    public void close() {
    	pool.close();
    }
    
    //取得权重值
    public double getWeight() {
    	//活动的连接数 除以 最大连接数
    	double numActive = pool.getNumActive(); 
    	double maxTotal = pool.getMaxTotal();
    	System.out.println(numActive + "**" + maxTotal); 
    	return numActive / maxTotal;
    }
    
    //从池里获取一个TProtocol对象
    public TProtocol getConnection() {  
        try {
			return pool.borrowObject();
		} catch (Exception e) {
			e.printStackTrace();
		} 
        return null;
    }  
    
    //把一个TProtocol对象归还到池里
    public void releaseConnection(TProtocol tProtocol) {  
        pool.returnObject(tProtocol);   
    }  
    
    /*
     * 连接池管理的对象TProtocol的工厂类，
     * GenericObjectPool会使用此类的create方法来
     * 创建TProtocol对象并放进pool里进行管理等操作。
     */
    class ConnectionFactory extends BasePooledObjectFactory<TProtocol> {   
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
        
        //创建TProtocol类型对象方法
        @Override
		public TProtocol create() throws Exception {
        	TTransport transport = null;
        	TSocket socket = null;
	        if (ssl == true) {
	            socket = TSSLTransportFactory.getClientSocket(host, port, 0);
	        } else {
	            socket = new TSocket(host, port);
	        }
	        socket.setTimeout(socketTimeout);
	        
	        transport = socket;
	        if (transportType.equals("buffered")) {
	        } else if (transportType.equals("framed")) {
	            transport = new TFramedTransport(transport);
	        } else if (transportType.equals("fastframed")) {
	            transport = new TFastFramedTransport(transport);
	        }
	        
	        if ( !transport.isOpen() ) {
	        	transport.open();
	        }
	        
	        TProtocol tProtocol = null; //通信协议
		    if (protocolType.equals("json")) {
		        tProtocol = new TJSONProtocol(transport);
		    } else if (protocolType.equals("compact")) {
		        tProtocol = new TCompactProtocol(transport);
		    } else {
		        tProtocol = new TBinaryProtocol(transport);
		    }
		    
			return tProtocol;
		}

        //把TProtocol对象打包成池管理的对象PooledObject<TProtocol>
		@Override
		public PooledObject<TProtocol> wrap(TProtocol tProtocol) {
			return new DefaultPooledObject<TProtocol>(tProtocol);
		}
		
		//销毁对象
		@Override
	    public void destroyObject(final PooledObject<TProtocol> p) throws Exception { 
			TTransport tTransport = p.getObject().getTransport();
			tTransport.flush();
			tTransport.close();
	    }
		
		//检查对象的有效性
		@Override
	    public boolean validateObject(final PooledObject<TProtocol> p) {
			TProtocol tProtocol = p.getObject();
			if ( tProtocol.getTransport().isOpen() ) {
				int seqid = 0;
				String methodName = "proxyValidate";
				String exceptionMsg = "Invalid method name: '" + methodName + "'";
				
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
					
					if (msg.seqid != seqid) {
					    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, methodName + " failed: out of sequence response");
					}
					tProtocol.readMessageEnd();
				} catch (Exception e) {
					e.printStackTrace();
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
