package com.halloffame.thriftjsoa.base;

import java.util.UUID;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;
import org.slf4j.MDC;

import com.halloffame.thriftjsoa.ThirftJsoaProxy;

public class ThirftJsoaProcessor implements TProcessor {
	
    private final TProcessor tProcessor;
    
    public ThirftJsoaProcessor(TProcessor tProcessor) {
    	this.tProcessor = tProcessor;
    }
    
    public boolean process(TProtocol iprot, TProtocol oprot) throws TException {
        
        TMessage message = iprot.readMessageBegin();

        if (message.type != TMessageType.CALL && message.type != TMessageType.ONEWAY) {
            throw new TException("This should not have happened!?");
        }

        String[] msgNameArr = message.name.split(ThirftJsoaProtocol.SEPARATOR);
        String appId = null;
        String traceId;
        String msgName;

        if (msgNameArr.length != 3) {
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
            msgName = message.name;
        } else {
            traceId = msgNameArr[0];
            appId = msgNameArr[1];

            if (tProcessor instanceof ThirftJsoaProxy.ProxyProcessor) {
                msgName = message.name;
            } else {
                msgName = msgNameArr[2];
            }
        }
        MDC.put(ThirftJsoaProtocol.TRACE_KEY, traceId);
        MDC.put(ThirftJsoaProtocol.APP_KEY, appId);
        
        TMessage standardMessage = new TMessage(msgName, message.type, message.seqid);

        return tProcessor.process(new StoredMessageProtocol(iprot, standardMessage), oprot);
    }
    
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
