package com.halloffame.thriftjsoa.common;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport; 
  
public class ConnectionPoolFactory {  
    private GenericObjectPool<TTransport> pool;  
    private String hostStr;
  
    public ConnectionPoolFactory(GenericObjectPoolConfig config, String host, int port, 
    		int socketTimeout, boolean ssl, String transportType) {  
    	hostStr = host + ":" + port;
    	ConnectionFactory objFactory = new ConnectionFactory(host, port, socketTimeout, ssl, transportType);  
        pool = new GenericObjectPool<TTransport>(objFactory, config);  
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
    
    //从池里获取一个Transport对象
    public TTransport getConnection() {  
        try {
			return pool.borrowObject();
		} catch (Exception e) {
			e.printStackTrace();
		} 
        return null;
    }  
    
    //把一个Transport对象归还到池里
    public void releaseConnection(TTransport transport) {  
        pool.returnObject(transport);   
    }  
    
    /*
     * 连接池管理的对象Transport的工厂类，
     * GenericObjectPool会使用此类的create方法来
     * 创建Transport对象并放进pool里进行管理等操作。
     */
    class ConnectionFactory extends BasePooledObjectFactory<TTransport> {   
    	private String host;  
        private int port;
        private int socketTimeout;
        private String transportType; 
        private boolean ssl;
          
        public ConnectionFactory(String host, int port, int socketTimeout, boolean ssl, String transportType) {  
        	this.host = host;
        	this.port = port;
        	this.socketTimeout = socketTimeout;
        	this.ssl = ssl;
        	this.transportType = transportType;
        }  
        
        //创建TTransport类型对象方法
        @Override
		public TTransport create() throws Exception {
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
			return transport;
		}

        //把TTransport对象打包成池管理的对象PooledObject<TTransport>
		@Override
		public PooledObject<TTransport> wrap(TTransport transport) {
			return new DefaultPooledObject<TTransport>(transport);
		}
    }
    
} 
