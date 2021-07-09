package com.halloffame.thriftjsoa.base;

import lombok.Data;
import org.apache.thrift.protocol.TProtocol;

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
    private TProtocol tProtocol;

    /**
     * 如果是TMultiplexedProtocol，多个客户端对应同一个TProtocol
     */
    private Map<Class<?>, TProtocol> tProtocolMap;
}
