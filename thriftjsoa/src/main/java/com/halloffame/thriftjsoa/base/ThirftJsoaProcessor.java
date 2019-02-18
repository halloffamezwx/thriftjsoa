package com.halloffame.thriftjsoa.base;

import java.util.UUID;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.slf4j.MDC;

import com.halloffame.thriftjsoa.ThirftJsoaProxy;

/**
 * ThirftJsoa的Processor，用于消息头的traceid和appid的解析处理
 */
public class ThirftJsoaProcessor implements TProcessor {
	
    private final TProcessor tProcessor; //封装的TProcessor
    private final String connValidateMethodName; //网络连通检查的请求的不存在的接口名
    
    public ThirftJsoaProcessor(TProcessor tProcessor, String connValidateMethodName) {
    	this.tProcessor = tProcessor;
    	this.connValidateMethodName = connValidateMethodName;
    }

    @Override
    public boolean process(TProtocol iprot, TProtocol oprot) throws TException {
        
        TMessage message = iprot.readMessageBegin();

        if (message.type != TMessageType.CALL && message.type != TMessageType.ONEWAY) {
            throw new TException("This should not have happened!?");
        }

        //网络连通检查，直接返回TApplicationException.UNKNOWN_METHOD
        if (connValidateMethodName.equals(message.name)) {
            TProtocolUtil.skip(iprot, TType.STRUCT);
            iprot.readMessageEnd();
            TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + message.name + "'");
            oprot.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
            x.write(oprot);
            oprot.writeMessageEnd();
            oprot.getTransport().flush();
            return true;
        }

        //消息头的组成是：traceId,appId,原始值
        String[] msgNameArr = message.name.split(ThirftJsoaProtocol.SEPARATOR);
        String appId = null;
        String traceId;
        String msgName;

        if (msgNameArr.length != 3) {
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
            msgName = message.name;
        } else {
            traceId = msgNameArr[0];
            if ("".equals(traceId) || "null".equals(traceId)) {
                traceId = UUID.randomUUID().toString().replaceAll("-", "");
            }
            appId = msgNameArr[1];

            if (tProcessor instanceof ThirftJsoaProxy.ProxyProcessor) {
                msgName = message.name;
            } else {
                msgName = msgNameArr[2];
            }
        }
        //把消息头的traceid和appid存放到MDC，这样任何地方的业务代码有需要的话就可以从MDC取出来
        MDC.put(ThirftJsoaProtocol.TRACE_KEY, traceId);
        MDC.put(ThirftJsoaProtocol.APP_KEY, appId);

        TMessage standardMessage = new TMessage(msgName, message.type, message.seqid);

        return tProcessor.process(new StoredMessageProtocol(iprot, standardMessage), oprot);
    }

    /**
     * TProtocol已经从流里面读取了消息头并处理过，需要重新装饰，让后面的流程读取正确的消息头
     */
    private static class StoredMessageProtocol extends TProtocolDecorator {
        TMessage messageBegin;
        
        public StoredMessageProtocol(TProtocol protocol, TMessage messageBegin) {
            super(protocol);
            this.messageBegin = messageBegin;
        }
        
        @Override
        public TMessage readMessageBegin() throws TException {
            return messageBegin;
        }
    }

}
