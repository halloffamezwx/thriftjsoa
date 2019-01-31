package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.base.ThirftJsoaException;
import com.halloffame.thriftjsoa.base.ThirftJsoaProtocol;
import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.*;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * 客户端工具类
 */
public class CommonClient {
    public static final ThreadLocal<Map<Class<? extends TServiceClient>, TServiceClient>> tServiceClientMapThreadLocal = new ThreadLocal<>();
    public static final String HTTP_URL_TEMPLATE = "http://%s:%s/service";

    /**
     * 从ThreadLocal里取得客户端
     */
    public static <T extends TServiceClient> T getClient(Class<T> clazz) {
        Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = tServiceClientMapThreadLocal.get();
        TServiceClient tServiceClient = tServiceClientMap.get(clazz);
        return (T)tServiceClient;
    }

    /**
     * 根据配置创建客户端
     */
    public static <T extends TServiceClient> T createClient(Class<T> clazz, ClientConfig clientConfig) throws Exception {
        String protocol_type = clientConfig.getProtocolType(); //需要和服务端的一致才能正常通信
        String transport_type = clientConfig.getTransportType(); //需要和服务端的一致才能正常通信
        boolean ssl = clientConfig.isSsl(); //传输是否加密
        int socketTimeout = clientConfig.getSocketTimeout(); //读超时时间
        String host = clientConfig.getHost(); //服务主机
        int port = clientConfig.getPort(); //服务端口

        //检查传入的变量值是否正确
        if (protocol_type.equals(ProtocolType.BINARY)) {
        } else if (protocol_type.equals(ProtocolType.COMPACT)) {
        } else if (protocol_type.equals(ProtocolType.JSON)) {
        } else {
            throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "Unknown protocol type! " + protocol_type);
        }

        if (transport_type.equals(TransportType.BUFFERED)) {
        } else if (transport_type.equals(TransportType.FRAMED)) {
        } else if (transport_type.equals(TransportType.FASTFRAMED)) {
        } else if (transport_type.equals(TransportType.HTTP)) {
        } else {
            throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "Unknown transport type! " + transport_type);
        }

        if (transport_type.equals(TransportType.HTTP) && ssl == true) { //不支持https
            throw new ThirftJsoaException(MsgCode.THRIFTJSOA_EXCEPTION, "SSL is not supported over http.");
        }

        TTransport transport; //指定的通信方式

        if (transport_type.equals(TransportType.HTTP)) {
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
            if (transport_type.equals(TransportType.BUFFERED)) {
            } else if (transport_type.equals(TransportType.FRAMED)) {
                transport = new TFramedTransport(transport);
            } else if (transport_type.equals(TransportType.FASTFRAMED)) {
                transport = new TFastFramedTransport(transport);
            }
        }

        TProtocol tProtocol;
        if (protocol_type.equals(ProtocolType.JSON)) {
            tProtocol = new TJSONProtocol(transport);
        } else if (protocol_type.equals(ProtocolType.COMPACT)) {
            tProtocol = new TCompactProtocol(transport);
        } else {
            tProtocol = new TBinaryProtocol(transport);
        }
        tProtocol = new ThirftJsoaProtocol(tProtocol);

        if (transport.isOpen() == false) {
            transport.open();
        }

        Constructor<T> constructor = clazz.getConstructor(TProtocol.class);
        T t = constructor.newInstance(tProtocol);

        return t;
    }

}
