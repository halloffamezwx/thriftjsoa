package com.halloffame.thriftjsoa.base;

import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.BaseClientConfig;
import com.halloffame.thriftjsoa.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.constant.MsgCode;
import com.halloffame.thriftjsoa.constant.ProtocolType;
import com.halloffame.thriftjsoa.constant.TransportType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 连接工厂类
 * @author zhuwx
 */
@Slf4j
public class ConnectionFactory {

    private final BaseClientConfig clientConfig;

    private static final int VALIDATE_SEQ_ID = 0; //检查TProtocol连接对象的有效性请求的seqid
    private final String validateExceptionMsg; //检查TProtocol连接对象的有效性期待服务端返回的错误消息

    /**
     * 主机 + "-" +  端口
     */
    private final String hostStr;

    /**
     * 连接池
     */
    private GenericObjectPool<TProtocol> pool;

    public ConnectionFactory(BaseClientConfig clientConfig) {
        //检查传入的变量值是否正确
        if (ProtocolType.getByCode(clientConfig.getProtocolType()) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, clientConfig.getProtocolType());
        }
        if (TransportType.getByCode(clientConfig.getTransportType()) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, clientConfig.getTransportType());
        }
        if (TransportType.HTTP.getCode().equals(clientConfig.getTransportType()) && clientConfig.isSsl()) { //不支持https
            throw new ThriftJsoaException(MsgCode.HTTPS_NOT_SUPPORT);
        }

        this.clientConfig = clientConfig;
        this.hostStr = clientConfig.getHost() + CommonServer.ZK_NODE_SEPARATOR + clientConfig.getPort();
        this.validateExceptionMsg = "Invalid method name: '" + clientConfig.getConnValidateMethodName() + "'";

        if (clientConfig.getPoolConfig() != null) {
            TProtocolPooledObjectFactory objFactory = new TProtocolPooledObjectFactory(this);
            pool = new GenericObjectPool<>(objFactory, clientConfig.getPoolConfig());
        }
    }

    @Override
    public String toString() {
        ZkConnConfig zkConnConfig = clientConfig.getZkConnConfig();
        if (zkConnConfig == null) {
            return hostStr;
        } else {
            String zkRootPath = zkConnConfig.getZkRootPath();
            String zkNodePath = zkConnConfig.getZkNodePath();
            if (zkNodePath == null || "".equals(zkNodePath.trim())) {
                return zkRootPath + "/" + hostStr;
            } else {
                return zkRootPath + zkNodePath;
            }
        }
    }

    public void destroy() {
        if (pool != null) {
            if (!pool.isClosed()) {
                pool.close();
            }
        }
    }

    /**
     * 取得最大连接数
     */
    public int getMaxTotal() {
        if (pool != null) {
            return pool.getMaxTotal();
        } else {
            return clientConfig.getMaxTotal();
        }
    }

    /**
     * 取得活动连接数
     */
    public int getNumActive() {
        if (pool != null) {
            return pool.getNumActive();
        } else {
            //todo 需要实现有个地方（例如zookeeper或者redis等）能统一拿到对应服务的活动连接数
            return 1;
        }
    }

    /**
     * 取得权重值：活动的连接数 除以 最大连接数
     */
    public double getWeight() {
        double numActive = getNumActive();
        double maxTotal = getMaxTotal();
        double result = numActive / maxTotal;
        log.debug("thriftjsoa ConnectionFactory {} getWeight: {} / {} = {}", hostStr, numActive, maxTotal, result);
        return result;
    }

    /**
     * 获取包装的连接对象
     */
    public TProtocolWrap getWrapConnection() {
        TProtocolWrap result = new TProtocolWrap();
        TProtocol tProtocol = getConnection();
        result.setTProtocol(tProtocol);
        Map<Class<?>, TProtocol> tProtocolMap = new HashMap<>();
        result.setTProtocolMap(tProtocolMap);

        if (clientConfig.getClazzs() != null) {
            boolean isMultiplexed = false;
            for (ClientClassConfig clazzIt : clientConfig.getClazzs()) {
                if (clazzIt.getServiceName() != null) {
                    isMultiplexed = true;
                    break;
                }
            }
            for (ClientClassConfig clazzIt : clientConfig.getClazzs()) {
                TProtocol tProtocolFinal = tProtocol;
                if (isMultiplexed) {
                    //如果是TMultiplexedProtocol，多个客户端对应同一个TProtocol
                    tProtocolFinal = new TMultiplexedProtocol(tProtocol, clazzIt.getServiceName());
                }
                tProtocolMap.put(clazzIt.getName(), tProtocolFinal);
            }
        }

        return result;
    }

    /**
     * 获取一个TProtocol连接对象
     */
    public TProtocol getConnection() {
        TProtocol tProtocol = null;
        try {
            if (pool != null) {
                tProtocol = pool.borrowObject();
            } else {
                tProtocol = create();
                //todo 需要实现有个地方（例如zookeeper或者redis等）能统一保存对应服务的活动连接数（累加一）
            }

            if (tProtocol != null && !tProtocol.getTransport().isOpen()) {
                tProtocol.getTransport().open();
            }
        } catch (Exception e) {
            log.error("thriftjsoa ConnectionFactory getConnection Exception: ", e);
        }
        return tProtocol;
    }

    /**
     * 释放TProtocol连接对象
     */
    public void releaseConnection(TProtocol tProtocol) {
        if (tProtocol != null) {
            if (pool != null) {
                pool.returnObject(tProtocol);
            } else {
                if (tProtocol.getTransport().isOpen()) {
                    tProtocol.getTransport().close();
                }
                //todo 需要实现有个地方（例如zookeeper或者redis等）能统一保存对应服务的活动连接数（累减一）
            }
        }
        //MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
        //MDC.remove(ThriftJsoaProtocol.APP_KEY);
    }

    /**
     * 创建TProtocol连接对象
     */
    public TProtocol create() throws Exception {
        String host = clientConfig.getHost();
        int port = clientConfig.getPort();
        String transportType = clientConfig.getTransportType();
        String httpPath = clientConfig.getHttpPath();
        String protocolType = clientConfig.getProtocolType();

        TTransport transport; //传输方式

        if (TransportType.HTTP.getCode().equals(transportType)) {
            String url = String.format(CommonClient.HTTP_URL_TEMPLATE, host, port, httpPath);
            transport = new THttpClient(url);
        } else {
            TSocket socket;
            if (clientConfig.isSsl()) {
                socket = TSSLTransportFactory.getClientSocket(host, port, 0);
            } else {
                socket = new TSocket(host, port);
            }
            socket.setTimeout(clientConfig.getSocketTimeOut());

            transport = socket;
            if (TransportType.BUFFERED.getCode().equals(transportType)) {

            } else if (TransportType.FRAMED.getCode().equals(transportType)) {
                transport = new TFramedTransport(transport);
            } else if (TransportType.FASTFRAMED.getCode().equals(transportType)) {
                transport = new TFastFramedTransport(transport);
            }
        }

        TProtocol tProtocol;
        if (ProtocolType.JSON.getCode().equals(protocolType)) {
            tProtocol = new TJSONProtocol(transport);
        } else if (ProtocolType.COMPACT.getCode().equals(protocolType)) {
            tProtocol = new TCompactProtocol(transport);
        } else {
            tProtocol = new TBinaryProtocol(transport);
        }

        if (clientConfig.isInTjServer()) { //在ThriftJsoa服务端发起连接请求
            tProtocol = new ThriftJsoaProtocol(tProtocol); //发起请求的时候从上下文环境变量mdc中读取traceId和appId并写到消息头
        } else {
            tProtocol = new ThriftJsoaClientProtocol(tProtocol); //接收数据的时候读取消息头的traceId和appId并存放到上下文环境变量mdc中以供业务需要来使用
        }

        if (!transport.isOpen()) {
            transport.open();
        }

        return tProtocol;
    }

    /**
     * 检查TProtocol连接对象的有效性
     * 请求一个不存在的接口，服务端会返回相应的错误信息，这样就可以判断此连接的链路是否相通
     */
    public boolean validateObject(final TProtocol tProtocol) {

        if ( tProtocol.getTransport().isOpen() ) {
            try {
                tProtocol.writeMessageBegin(new TMessage(clientConfig.getConnValidateMethodName(), TMessageType.CALL, VALIDATE_SEQ_ID));
                tProtocol.writeStructBegin(new TStruct(""));
                tProtocol.writeFieldStop();
                tProtocol.writeStructEnd();
                tProtocol.writeMessageEnd();
                tProtocol.getTransport().flush();

                TMessage msg = tProtocol.readMessageBegin();
                if (msg.seqid != VALIDATE_SEQ_ID) {
                    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, msg.name + " failed: out of sequence response");
                }
                if (msg.type == TMessageType.EXCEPTION) {
                    TApplicationException x = TApplicationException.read(tProtocol);
                    tProtocol.readMessageEnd();
                    if (x.getType() == TApplicationException.UNKNOWN_METHOD && validateExceptionMsg.equals(x.getMessage())) {
                        return true;
                    } else {
                        throw x;
                    }
                }
            } catch (TException e) {
                log.warn("thriftjsoa ConnectionFactory validateObject TException: ", e);
            }
        }

        return false;
    }

    /**
     * 连接池管理的连接对象TProtocol的工厂类，
     * GenericObjectPool会使用此类的create方法来
     * 创建TProtocol连接对象并放进pool里进行管理等操作。
     */
    class TProtocolPooledObjectFactory extends BasePooledObjectFactory<TProtocol> {

        private final ConnectionFactory connectionFactory;

        public TProtocolPooledObjectFactory (ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        /**
         * 创建TProtocol连接对象
         */
        @Override
        public TProtocol create() throws Exception {
            return connectionFactory.create();
        }

        /**
         * 把TProtocol连接对象打包成池管理的对象PooledObject<TProtocol>
         */
        @Override
        public PooledObject<TProtocol> wrap(TProtocol tProtocol) {
            return new DefaultPooledObject<>(tProtocol);
        }

        /**
         * 销毁TProtocol连接对象
         */
        @Override
        public void destroyObject(final PooledObject<TProtocol> p) throws Exception {
            TTransport tTransport = p.getObject().getTransport();
            tTransport.flush();
            tTransport.close();
        }

        /**
         * 检查TProtocol连接对象的有效性
         * 请求一个不存在的接口，服务端会返回相应的错误信息，这样就可以判断此连接的链路是否相通
         */
        @Override
        public boolean validateObject(final PooledObject<TProtocol> p) {
            return connectionFactory.validateObject(p.getObject());
        }

        //borrowObject时触发
        //@Override
        //public void activateObject(final PooledObject<TProtocol> p) throws Exception { }
        //returnObject时触发
        //@Override
        //public void passivateObject(final PooledObject<TProtocol> p) throws Exception { }
    }
} 
