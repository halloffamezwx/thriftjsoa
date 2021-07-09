package com.halloffame.thriftjsoa.base;

import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.slf4j.MDC;

/**
 * ThriftJsoa的Processor，用于消息头的traceId和appId的解析处理 以及 链接的连通有效性检查
 * @author zhuwx
 */
public class ThriftJsoaProcessor implements TProcessor {

    /**
     * 封装的TProcessor
     */
    private final TProcessor tProcessor;

    /**
     * 链接的连通有效性检查的请求的不存在的接口名
     */
    private final String connValidateMethodName;
    
    public ThriftJsoaProcessor(TProcessor tProcessor, String connValidateMethodName) {
    	this.tProcessor = tProcessor;
    	this.connValidateMethodName = connValidateMethodName;
    }

    @Override
    public boolean process(TProtocol iprot, TProtocol oprot) throws TException {
        TMessage message = iprot.readMessageBegin();
        if (message.type != TMessageType.CALL && message.type != TMessageType.ONEWAY) {
            throw new TException("This should not have happened!?");
        }

        //链接的连通有效性检查，直接返回TApplicationException.UNKNOWN_METHOD
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

        //消息头的组成是：traceId + "," + appId + "," + 原始值
        String[] msgNameArr = message.name.split(ThriftJsoaProtocol.FIELD_SEPARATOR);
        String appId = null;
        String traceId;
        String msgName;

        if (msgNameArr.length != ThriftJsoaProtocol.FIELD_SIZE) {
            traceId = ThriftJsoaUtil.genUuid();
            msgName = message.name;
        } else {
            traceId = msgNameArr[ThriftJsoaProtocol.TRACE_ID_FIELD_INDEX];
            appId = msgNameArr[ThriftJsoaProtocol.APP_ID_FIELD_INDEX];

            if (traceId == null || "".equals(traceId.trim()) || "null".equalsIgnoreCase(traceId.trim())) {
                traceId = ThriftJsoaUtil.genUuid();
            }

            msgName = msgNameArr[ThriftJsoaProtocol.ORIGINAL_FIELD_INDEX];
        }
        //把消息头的traceId和appId存放到MDC，这样任何地方的业务代码有需要的话就可以从MDC取出来
        MDC.put(ThriftJsoaProtocol.TRACE_KEY, traceId);
        MDC.put(ThriftJsoaProtocol.APP_KEY, appId);

        TMessage standardMessage = new TMessage(msgName, message.type, message.seqid);
        return tProcessor.process(new StoredMessageProtocol(iprot, standardMessage), oprot);
    }

    /**
     * TProtocol已经从流里面读取了消息头并处理过，需要重新装饰，让后面的流程继续读取正确的消息头
     */
    private static class StoredMessageProtocol extends TProtocolDecorator {

        /**
         * 封装的消息头
         */
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
