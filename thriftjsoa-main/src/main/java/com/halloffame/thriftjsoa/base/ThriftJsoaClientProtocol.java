package com.halloffame.thriftjsoa.base;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;
import org.slf4j.MDC;

/**
 * ThriftJsoa的客户端Protocol，用于获取消息头的traceid
 * @author zhuwx
 */
public class ThriftJsoaClientProtocol extends TProtocolDecorator {

    public ThriftJsoaClientProtocol(TProtocol protocol) {
        super(protocol);
    }

    /**
     * 读消息头
     */
    @Override
    public TMessage readMessageBegin() throws TException {
        TMessage message = super.readMessageBegin();

        //消息头的组成是：traceId + "," + appId + "," + 原始值
        String[] msgNameArr = message.name.split(ThriftJsoaProtocol.FIELD_SEPARATOR);
        String msgName;

        if (ThriftJsoaProtocol.FIELD_SIZE == msgNameArr.length) {
            msgName = msgNameArr[ThriftJsoaProtocol.ORIGINAL_FIELD_INDEX];
            //把消息头的traceid和appid存放到MDC，这样任何地方的业务代码有需要的话就可以从MDC取出来
            MDC.put(ThriftJsoaProtocol.TRACE_KEY, msgNameArr[ThriftJsoaProtocol.TRACE_ID_FIELD_INDEX]);
            MDC.put(ThriftJsoaProtocol.APP_KEY, msgNameArr[ThriftJsoaProtocol.APP_ID_FIELD_INDEX]);
        } else {
            msgName = message.name;
        }

        return new TMessage(msgName, message.type, message.seqid);
    }
    
}
