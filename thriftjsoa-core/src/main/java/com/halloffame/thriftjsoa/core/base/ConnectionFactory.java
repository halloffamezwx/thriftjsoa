package com.halloffame.thriftjsoa.core.base;

import com.halloffame.thriftjsoa.core.common.CommonClient;
import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.config.BaseClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.constant.MsgCode;
import com.halloffame.thriftjsoa.core.constant.ProtocolType;
import com.halloffame.thriftjsoa.core.constant.TransportType;
import com.halloffame.thriftjsoa.core.protocol.TAvroProtocol;
import com.halloffame.thriftjsoa.core.protocol.TKryoProtocol;
import com.halloffame.thriftjsoa.core.protocol.TProtostuffProtocol;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.*;

/**
 * 连接工厂类
 * @author zhuwx
 */
@Slf4j
public class ConnectionFactory {

    private final BaseClientConfig clientConfig;

    private static final int VALIDATE_SEQ_ID = 0; //检查连接对象的有效性请求的seqid
    private static final int SHUTDOWN_GRACEFUL_SEQ_ID = 0;
    private final String validateExceptionMsg; //检查连接对象的有效性期待服务端返回的错误消息

    /**
     * 服务app标识
     */
    private final String appId;

    /**
     * 连接池
     */
    private GenericObjectPool<TTransport> pool;

    public ConnectionFactory(BaseClientConfig clientConfig) {
        //todo 如果必要参数比如请求的主机和端口等未传入，就从注册中心获取

        //检查传入的变量值是否正确
        String inTProtocolType = clientConfig.getInProtocolType() == null ? clientConfig.getGeneralProtocolType() : clientConfig.getInProtocolType();
        String outTProtocolType = clientConfig.getOutProtocolType() == null ? clientConfig.getGeneralProtocolType() : clientConfig.getOutProtocolType();
        if (ProtocolType.getByCode(inTProtocolType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, inTProtocolType);
        }
        if (ProtocolType.getByCode(outTProtocolType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, outTProtocolType);
        }

        String inTTransportType = clientConfig.getInTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getInTransportType();
        String outTTransportType = clientConfig.getOutTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getOutTransportType();
        if (TransportType.getByCode(inTTransportType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, inTTransportType);
        }
        if (TransportType.getByCode(outTTransportType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, outTTransportType);
        }
        if ((TransportType.HTTP.getCode().equals(inTTransportType) || TransportType.HTTP.getCode().equals(outTTransportType)) && clientConfig.isSsl()) { //不支持https
            throw new ThriftJsoaException(MsgCode.HTTPS_NOT_SUPPORT);
        }

        this.clientConfig = clientConfig;
        this.appId = CommonServer.genAppId(clientConfig);
        this.validateExceptionMsg = "Invalid method name: '" + clientConfig.getConnValidateMethodName() + "'";

        if (clientConfig.getPoolConfig() != null) {
            PooledObjectFactory objFactory = new PooledObjectFactory(this);
            pool = new GenericObjectPool<>(objFactory, clientConfig.getPoolConfig());
        }
    }

    @Override
    public String toString() {
        if (clientConfig.getZkRegisterConfig() != null) {
            ZkRegisterConfig zkRegisterConfig = clientConfig.getZkRegisterConfig();
            if (zkRegisterConfig != null) {
                return String.format("%s, %s, %s", zkRegisterConfig.getZkRootPath(), zkRegisterConfig.getZkNodePath(), appId);
            }
        }
        return String.format("%s, %s, %s", null, null, appId);
    }

    /**
     * 是否同一个连接工厂
     */
    public boolean isSame(String path, String appIdIn) {
        String zkPath = null;
        if (clientConfig.getZkRegisterConfig() != null) {
            ZkRegisterConfig zkRegisterConfig = clientConfig.getZkRegisterConfig();
            if (zkRegisterConfig != null) {
                zkPath = zkRegisterConfig.getZkRootPath() + zkRegisterConfig.getZkNodePath();
            }
        }

        if (Objects.equals(path, zkPath) || Objects.equals(path, this.appId) ||
                Objects.equals(appIdIn, this.appId) || Objects.equals(appIdIn, zkPath)) {
            return true;
        }

        return false;
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
        log.debug("thriftjsoa ConnectionFactory {} getWeight: {} / {} = {}", this, numActive, maxTotal, result);
        return result;
    }

    /**
     * 释放连接对象
     */
    public void releaseConnection(TTransport tTransport) {
        if (tTransport != null) {
            if (pool != null) {
                pool.returnObject(tTransport);
            } else {
                if (tTransport.isOpen()) {
                    tTransport.close();
                }
                //todo 需要实现有个地方（例如zookeeper或者redis等）能统一保存对应服务的活动连接数（累减一）
            }
        }
    }

    /**
     * 检查连接对象的有效性
     * 请求一个不存在的接口，服务端会返回相应的错误信息，这样就可以判断此连接的链路是否相通
     */
    public boolean validateObject(final TTransport tTransport) {
        if (tTransport.isOpen()) {
            try {
                TProtocolWrap tProtocolWrap = getWrapConnection(tTransport);
                TProtocol outTProtocol = tProtocolWrap.getOutTjProtocol();
                TProtocol inTProtocol = tProtocolWrap.getInTjProtocol();

                TMessage tMessage = new TMessage(clientConfig.getConnValidateMethodName(), TMessageType.CALL, VALIDATE_SEQ_ID);
                outTProtocol.writeMessageBegin(tMessage);
                outTProtocol.writeStructBegin(new TStruct(""));
                outTProtocol.writeFieldStop();
                outTProtocol.writeStructEnd();
                outTProtocol.writeMessageEnd();
                outTProtocol.getTransport().flush();

                TMessage msg;
                msg = inTProtocol.readMessageBegin();

                if (msg.seqid != VALIDATE_SEQ_ID) {
                    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, msg.name + " failed: out of sequence response");
                }
                if (msg.type == TMessageType.EXCEPTION) {
                    TApplicationException x = TApplicationException.read(inTProtocol);
                    inTProtocol.readMessageEnd();
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
     * 优雅关机
     */
    public void shutdownGraceful() throws TException {
        TProtocolWrap tProtocolWrap = getWrapConnection();
        try {
            TjProtocol outTProtocol = tProtocolWrap.getOutTjProtocol();
            TjProtocol inTProtocol = tProtocolWrap.getInTjProtocol();

            TMessage tMessage = new TMessage(clientConfig.getShutdownGracefulMethodName(), TMessageType.CALL, SHUTDOWN_GRACEFUL_SEQ_ID);
            outTProtocol.writeMessageBegin(tMessage);
            outTProtocol.writeStructBegin(new TStruct(""));
            outTProtocol.writeFieldStop();
            outTProtocol.writeStructEnd();
            outTProtocol.writeMessageEnd();
            outTProtocol.getTransport().flush();

            TMessage msg;
            msg = inTProtocol.readMessageBegin();

            if (msg.seqid != VALIDATE_SEQ_ID) {
                throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, msg.name + " failed: out of sequence response");
            }
            if (msg.type == TMessageType.EXCEPTION) {
                TApplicationException x = TApplicationException.read(inTProtocol);
                inTProtocol.readMessageEnd();
                throw x;
            }

            TProtocolUtil.skip(inTProtocol, TType.STRUCT);
            inTProtocol.readMessageEnd();
        } finally {
            releaseConnection(tProtocolWrap.getTTransport());
        }
    }

    /**
     * 获取服务状态
     */
    @Deprecated
    public void getServerStatus() {
    }

    /**
     * 获取包装的连接对象
     */
    public TProtocolWrap getWrapConnection() {
        return getWrapConnection(null);
    }

    /**
     * 获取包装的连接对象
     */
    @SneakyThrows
    public TProtocolWrap getWrapConnection(TTransport tTransport) {
        if (tTransport == null) {
            if (pool != null) {
                tTransport = pool.borrowObject();
            } else {
                tTransport = create();
                //todo 需要实现有个地方（例如zookeeper或者redis等）能统一保存对应服务的活动连接数（累加一）
            }
        }
        if (tTransport != null && !tTransport.isOpen()) {
            tTransport.open();
        }

        TTransport inTTransport = null;
        TTransport outTTransport = null;
        String inTTransportType = clientConfig.getInTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getInTransportType();
        String outTTransportType = clientConfig.getOutTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getOutTransportType();

        if (TransportType.BUFFERED.getCode().equals(inTTransportType)) {

        } else if (TransportType.FRAMED.getCode().equals(inTTransportType)) {
            inTTransport = new TFramedTransport(tTransport);
        } else if (TransportType.FASTFRAMED.getCode().equals(inTTransportType)) {
            inTTransport = new TFastFramedTransport(tTransport);
        } else if (TransportType.KRYO.getCode().equals(inTTransportType)) {
            inTTransport = new TKryoTransport((TSocket) tTransport);
        }

        if (TransportType.BUFFERED.getCode().equals(outTTransportType)) {

        } else if (TransportType.FRAMED.getCode().equals(outTTransportType)) {
            outTTransport = new TFramedTransport(tTransport);
        } else if (TransportType.FASTFRAMED.getCode().equals(outTTransportType)) {
            outTTransport = new TFastFramedTransport(tTransport);
        } else if (TransportType.KRYO.getCode().equals(outTTransportType)) {
            outTTransport = new TKryoTransport((TSocket) tTransport);
        }

        TProtocol inTProtocol;
        TProtocol outTProtocol;
        String inTProtocolType = clientConfig.getInProtocolType() == null ? clientConfig.getGeneralProtocolType() : clientConfig.getInProtocolType();
        String outTProtocolType = clientConfig.getOutProtocolType() == null ? clientConfig.getGeneralProtocolType() : clientConfig.getOutProtocolType();

        TKryoProtocol inTKryoProtocol = null;
        if (ProtocolType.JSON.getCode().equals(inTProtocolType)) {
            inTProtocol = new TJSONProtocol(inTTransport);
        } else if (ProtocolType.COMPACT.getCode().equals(inTProtocolType)) {
            inTProtocol = new TCompactProtocol(inTTransport);
        } else if (ProtocolType.KRYO.getCode().equals(inTProtocolType)) {
            inTKryoProtocol = new TKryoProtocol(inTTransport);
            inTProtocol = inTKryoProtocol;

        } else if (ProtocolType.PROTOSTUFF.getCode().equals(inTProtocolType)) {
            inTProtocol = new TProtostuffProtocol(inTTransport);
        } else if (ProtocolType.AVRO.getCode().equals(inTProtocolType)) {
            inTProtocol = new TAvroProtocol(inTTransport);
        } else {
            inTProtocol = new TBinaryProtocol(inTTransport);
        }

        TKryoProtocol outTKryoProtocol = null;
        if (ProtocolType.JSON.getCode().equals(outTProtocolType)) {
            outTProtocol = new TJSONProtocol(outTTransport);
        } else if (ProtocolType.COMPACT.getCode().equals(outTProtocolType)) {
            outTProtocol = new TCompactProtocol(outTTransport);
        } else if (ProtocolType.KRYO.getCode().equals(outTProtocolType)) {
            outTKryoProtocol = new TKryoProtocol(outTTransport);
            outTProtocol = outTKryoProtocol;

        } else if (ProtocolType.PROTOSTUFF.getCode().equals(outTProtocolType)) {
            outTProtocol = new TProtostuffProtocol(outTTransport);
        } else if (ProtocolType.AVRO.getCode().equals(outTProtocolType)) {
            outTProtocol = new TAvroProtocol(outTTransport);
        } else {
            outTProtocol = new TBinaryProtocol(outTTransport);
        }

        TjProtocol inTjProtocol;
        TjProtocol outTjProtocol;

        if (clientConfig.isInTjServer()) { //在ThriftJsoa服务端发起连接请求
            outTjProtocol = new ThriftJsoaProtocol(outTProtocol); //发起请求的时候从上下文环境变量mdc中读取traceId和appId并写到消息头
            inTjProtocol = new ThriftJsoaProtocol(inTProtocol);
        } else {
            inTjProtocol = new ThriftJsoaClientProtocol(inTProtocol); //接收数据的时候读取消息头的traceId和appId并存放到上下文环境变量mdc中以供业务需要来使用
            outTjProtocol = new ThriftJsoaClientProtocol(outTProtocol);
        }

        TProtocolWrap result = new TProtocolWrap();
        result.setTTransport(tTransport);
        //result.setInTTransport(inTTransport);
        //result.setOutTTransport(outTTransport);
        //result.setInTProtocol(inTProtocol);
        //result.setOutTProtocol(outTProtocol);
        result.setInTjProtocol(inTjProtocol);
        result.setOutTjProtocol(outTjProtocol);

        Map<Class<?>, TjProtocol> inTProtocolMap = new HashMap<>();
        Map<Class<?>, TjProtocol> outTProtocolMap = new HashMap<>();
        result.setInTProtocolMap(inTProtocolMap);
        result.setOutTProtocolMap(outTProtocolMap);

        if (clientConfig.getClazzs() != null) {
            boolean isMultiplexed = false;

            for (ClientClassConfig clazzIt : clientConfig.getClazzs()) {
                if (clazzIt.getServiceName() != null && !"".equals(clazzIt.getServiceName().trim())) {
                    isMultiplexed = true;
                    //break;
                }
                if (inTKryoProtocol != null || outTKryoProtocol != null) {
                    Set<Class<?>> clazzs = new HashSet<>(ThriftJsoaUtil.getMethodStructTypes(clazzIt.getSessionName()));

                    if (clazzIt.getName() != null) {
                        for (Class<?> interfaceClass : clazzIt.getName().getInterfaces()) {
                            clazzs.addAll(ThriftJsoaUtil.getMethodStructTypes(interfaceClass));
                        }
                    }

                    if (inTKryoProtocol != null) {
                        inTKryoProtocol.addClazzs(clazzs);
                    }
                    if (outTKryoProtocol != null) {
                        outTKryoProtocol.addClazzs(clazzs);
                    }
                }
            }

            for (ClientClassConfig clazzIt : clientConfig.getClazzs()) {
                TjProtocol inTProtocolFinal = inTjProtocol;
                TjProtocol outTProtocolFinal = outTjProtocol;
                if (isMultiplexed) {
                    inTProtocolFinal = new TjMultiplexedProtocol(inTjProtocol, clazzIt.getServiceName());
                    outTProtocolFinal = new TjMultiplexedProtocol(outTjProtocol, clazzIt.getServiceName());
                }
                inTProtocolMap.put(clazzIt.getName(), inTProtocolFinal);
                outTProtocolMap.put(clazzIt.getName(), outTProtocolFinal);
                inTProtocolMap.put(clazzIt.getSessionName(), inTProtocolFinal);
                outTProtocolMap.put(clazzIt.getSessionName(), outTProtocolFinal);
            }
        }

        return result;
    }

    /**
     * 创建连接对象
     */
    @SneakyThrows
    public TTransport create() {
        String host = clientConfig.getHost();
        int port = clientConfig.getPort();
        String httpPath = clientConfig.getHttpPath();
        String inTTransportType = clientConfig.getInTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getInTransportType();
        String outTTransportType = clientConfig.getOutTransportType() == null ? clientConfig.getGeneralTransportType() : clientConfig.getOutTransportType();

        TTransport transport; //传输方式

        if (TransportType.HTTP.getCode().equals(inTTransportType) || TransportType.HTTP.getCode().equals(outTTransportType)) {
            String url = String.format(CommonClient.HTTP_URL_TEMPLATE, host, port, httpPath);
            THttpClient tHttpClient = new THttpClient(url);
            tHttpClient.setReadTimeout(clientConfig.getSocketTimeOut());
            transport = tHttpClient;
        } else {
            TSocket socket;
            if (clientConfig.isSsl()) {
                socket = TSSLTransportFactory.getClientSocket(host, port, 0);
            } else {
                socket = new TSocket(host, port);
            }
            socket.setTimeout(clientConfig.getSocketTimeOut());

            transport = socket;
        }
        if (!transport.isOpen()) {
            transport.open();
        }

        return transport;
    }

    /**
     * 连接池管理的连接对象的工厂类，
     * GenericObjectPool会使用此类的create方法来
     * 创建连接对象并放进pool里进行管理等操作。
     */
    class PooledObjectFactory extends BasePooledObjectFactory<TTransport> {

        private final ConnectionFactory connectionFactory;

        public PooledObjectFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        /**
         * 创建连接对象
         */
        @Override
        public TTransport create() throws Exception {
            return connectionFactory.create();
        }

        /**
         * 把连接对象打包成池管理的对象PooledObject<>
         */
        @Override
        public PooledObject<TTransport> wrap(TTransport tTransport) {
            return new DefaultPooledObject<>(tTransport);
        }

        /**
         * 销毁连接对象
         */
        @Override
        public void destroyObject(final PooledObject<TTransport> p) throws Exception {
            TTransport tTransport = p.getObject(); //p.getObject().getTransport();
            tTransport.flush();
            tTransport.close();
        }

        /**
         * 检查连接对象的有效性
         * 请求一个不存在的接口，服务端会返回相应的错误信息，这样就可以判断此连接的链路是否相通
         */
        @Override
        public boolean validateObject(final PooledObject<TTransport> p) {
            return connectionFactory.validateObject(p.getObject());
        }

        //borrowObject时触发
        //@Override
        //public void activateObject(final PooledObject<TTransport> p) throws Exception { }
        //returnObject时触发
        //@Override
        //public void passivateObject(final PooledObject<TTransport> p) throws Exception { }
    }
} 
