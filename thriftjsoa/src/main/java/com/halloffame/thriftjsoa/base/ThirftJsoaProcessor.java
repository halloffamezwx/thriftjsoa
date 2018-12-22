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

        int index = message.name.indexOf(ThirftJsoaProtocol.SEPARATOR);
        String traceId = null;
        String msgName = null;
        
        if (index < 0) {
        	traceId = UUID.randomUUID().toString().replaceAll("-", "");
        	msgName = message.name;
        } else {
        	traceId = message.name.substring(0, index);
        	
        	if (tProcessor instanceof ThirftJsoaProxy.ProxyProcessor) {
        		msgName = message.name;
        	} else {
        		msgName = message.name.substring(traceId.length() + ThirftJsoaProtocol.SEPARATOR.length());
        	}
        }
        MDC.put(ThirftJsoaProtocol.THIRFTJSOA_TRACE_KEY, traceId);
        
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
