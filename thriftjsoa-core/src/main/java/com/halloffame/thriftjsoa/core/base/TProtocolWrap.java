package com.halloffame.thriftjsoa.core.base;

import lombok.Data;
import org.apache.thrift.transport.TTransport;

import java.util.Map;

/**
 * TProtocol包装类
 * @author zhuwx
 */
@Data
public class TProtocolWrap {

    /**
     * TProtocol通信协议连接对象
     */
    //private TProtocol inTProtocol;
    //private TProtocol outTProtocol;

    /**
     * TjProtocol通信协议连接对象
     */
    private TjProtocol inTjProtocol;
    private TjProtocol outTjProtocol;

    /**
     * 客户端TProtocol
     */
    private Map<Class<?>, TjProtocol> inTProtocolMap;
    private Map<Class<?>, TjProtocol> outTProtocolMap;

    /**
     * 连接对象
     */
    private TTransport tTransport;
    //private TTransport inTTransport;
    //private TTransport outTTransport;
}
