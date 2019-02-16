package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.base.ThirftJsoaException;
import com.halloffame.thriftjsoa.base.ThirftJsoaProtocol;
import com.halloffame.thriftjsoa.config.ClientClassConfig;
import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * 客户端工具类
 */
public class CommonClient {
    public static final ThreadLocal<Map<Class<? extends TServiceClient>, TServiceClient>> T_SERVICE_CLIENT_MAP_THREAD_LOCAL = new ThreadLocal<>(); //客户端上下文变量
    public static final String HTTP_URL_TEMPLATE = "http://%s:%s/service"; //客户端transport_type为http的url模板，第一个通配符是host，第二个通配符是port

    /**
     * 从ThreadLocal里取得客户端
     */
    public static <T extends TServiceClient> T getClient(Class<T> clazz) {
        Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = T_SERVICE_CLIENT_MAP_THREAD_LOCAL.get();
        TServiceClient tServiceClient = tServiceClientMap.get(clazz);
        return (T)tServiceClient;
    }

    /**
     * 根据配置创建客户端
     */
    public static <T extends TServiceClient> T createClient(Class<T> clazz, ClientConfig clientConfig,
                                                            Map<Class<? extends TServiceClient>, TProtocol> tServiceClientProtocolMap,
                                                            Map<TProtocol, ConnectionPoolFactory> tProtocolPoolMap) throws Exception {
        TProtocol tProtocol = null;
        ConnectionPoolFactory poolFactory = clientConfig.getPoolFactory(); //连接池，没有配置就为null
        String serviceName = null; //TMultiplexedProtocol的SERVICE_NAME

        for (ClientClassConfig clientClassConfig : clientConfig.getClientClassConfigs()) {
            if (clazz == clientClassConfig.getClazz()) {
                serviceName = clientClassConfig.getName();
            } else {
                tProtocol = tServiceClientProtocolMap.get(clientClassConfig.getClazz());
            }
        }

        if (tProtocol == null) {
            if (poolFactory != null) {
                tProtocol = poolFactory.getConnection();
            }
        }

        if (tProtocol == null) {
            String protocol_type = clientConfig.getProtocolType(); //需要和服务端的一致才能正常通信
            String transport_type = clientConfig.getTransportType(); //需要和服务端的一致才能正常通信
            boolean ssl = clientConfig.isSsl(); //传输是否加密
            int socketTimeout = clientConfig.getSocketTimeout(); //读超时时间
            String host = clientConfig.getHost(); //服务主机
            int port = clientConfig.getPort(); //服务端口

            //检查传入的变量值是否正确
            if (protocol_type.equals(ProtocolType.BINARY.getValue())) {
            } else if (protocol_type.equals(ProtocolType.COMPACT.getValue())) {
            } else if (protocol_type.equals(ProtocolType.JSON.getValue())) {
            } else {
                throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "Unknown protocol type! " + protocol_type);
            }

            if (transport_type.equals(TransportType.BUFFERED.getValue())) {
            } else if (transport_type.equals(TransportType.FRAMED.getValue())) {
            } else if (transport_type.equals(TransportType.FASTFRAMED.getValue())) {
            } else if (transport_type.equals(TransportType.HTTP.getValue())) {
            } else {
                throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "Unknown transport type! " + transport_type);
            }

            if (transport_type.equals(TransportType.HTTP.getValue()) && ssl == true) { //不支持https
                throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "SSL is not supported over http.");
            }

            TTransport transport; //指定的通信方式

            if (transport_type.equals(TransportType.HTTP.getValue())) {
                String url = String.format(HTTP_URL_TEMPLATE, host, port);
                transport = new THttpClient(url);
            } else {
                TSocket socket;
                if (ssl == true) {
                    socket = TSSLTransportFactory.getClientSocket(host, port, 0);
                } else {
                    socket = new TSocket(host, port);
                }

                socket.setTimeout(socketTimeout);

                transport = socket;
                if (transport_type.equals(TransportType.BUFFERED.getValue())) {
                } else if (transport_type.equals(TransportType.FRAMED.getValue())) {
                    transport = new TFramedTransport(transport);
                } else if (transport_type.equals(TransportType.FASTFRAMED.getValue())) {
                    transport = new TFastFramedTransport(transport);
                }
            }

            if (protocol_type.equals(ProtocolType.JSON.getValue())) {
                tProtocol = new TJSONProtocol(transport);
            } else if (protocol_type.equals(ProtocolType.COMPACT.getValue())) {
                tProtocol = new TCompactProtocol(transport);
            } else {
                tProtocol = new TBinaryProtocol(transport);
            }

            if (transport.isOpen() == false) {
                transport.open();
            }
        }

        tServiceClientProtocolMap.put(clazz, tProtocol); //如果是TMultiplexedProtocol，多个客户端对应同一个TProtocol
        tProtocolPoolMap.put(tProtocol, poolFactory); //最后需要释放资源的TProtocol和配置的连接池（可能为null代表没有配置）

        TProtocol finalProtocol = new ThirftJsoaProtocol(tProtocol);
        if (clientConfig.getClientClassConfigs().size() > 1) {
            finalProtocol = new TMultiplexedProtocol(finalProtocol, serviceName);
        }

        Constructor<T> constructor = clazz.getConstructor(TProtocol.class);
        T t = constructor.newInstance(finalProtocol);

        return t;
    }

}
