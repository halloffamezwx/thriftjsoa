package com.halloffame.thriftjsoa.common;

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

public class CommonClient {
    public static final ThreadLocal<Map<Class<? extends TServiceClient>, TServiceClient>> tServiceClientMapThreadLocal = new ThreadLocal<>();

    public static <T extends TServiceClient> T getClient(Class<T> clazz) {
        Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = tServiceClientMapThreadLocal.get();
        TServiceClient tServiceClient = tServiceClientMap.get(clazz);
        return (T)tServiceClient;
    }

    public static <T extends TServiceClient> T createClient(Class<T> clazz, ClientConfig clientConfig) throws Exception {
        String protocol_type = clientConfig.getProtocolType(); //需要和服务端的一致才能正常通信
        String transport_type = clientConfig.getTransportType(); //需要和服务端的一致才能正常通信
        boolean ssl = clientConfig.isSsl(); //传输是否加密
        int socketTimeout = clientConfig.getSocketTimeout();
        String host = clientConfig.getHost();
        int port = clientConfig.getPort();

        //检查传入的变量值是否正确
        if (protocol_type.equals("binary")) {
        } else if (protocol_type.equals("compact")) {
        } else if (protocol_type.equals("json")) {
        } else {
            throw new Exception("Unknown protocol type! " + protocol_type);
        }

        if (transport_type.equals("buffered")) {
        } else if (transport_type.equals("framed")) {
        } else if (transport_type.equals("fastframed")) {
        } else if (transport_type.equals("http")) {
        } else {
            throw new Exception("Unknown transport type! " + transport_type);
        }

        if (transport_type.equals("http") && ssl == true) { //不支持https
            throw new Exception("SSL is not supported over http.");
        }

        TTransport transport = null; //指定的通信方式

        if (transport_type.equals("http")) {
            String url = "http://" + host + ":" + port + "/service";
            transport = new THttpClient(url);
        } else {
            TSocket socket = null;
            if (ssl == true) {
                socket = TSSLTransportFactory.getClientSocket(host, port, 0);
            } else {
                socket = new TSocket(host, port);
            }

            socket.setTimeout(socketTimeout);

            transport = socket;
            if (transport_type.equals("buffered")) {
            } else if (transport_type.equals("framed")) {
                transport = new TFramedTransport(transport);
            } else if (transport_type.equals("fastframed")) {
                transport = new TFastFramedTransport(transport);
            }
        }

        TProtocol tProtocol = null;
        if (protocol_type.equals("json")) {
            tProtocol = new TJSONProtocol(transport);
        } else if (protocol_type.equals("compact")) {
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
