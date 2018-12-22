package com.halloffame.thriftjsoa.base;

import java.util.UUID;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.slf4j.MDC;

public class ThirftJsoaProtocol extends TProtocolDecorator {

    public static final String SEPARATOR = "-";
    
    public static final String THIRFTJSOA_TRACE_KEY = "thirftjsoa_trace_id";

    public ThirftJsoaProtocol(TProtocol protocol) {
        super(protocol);
    }

    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {
    	
        if (tMessage.type == TMessageType.CALL || tMessage.type == TMessageType.ONEWAY || tMessage.type == TMessageType.REPLY) {
        	String traceId = MDC.get(THIRFTJSOA_TRACE_KEY);
        	
        	if (traceId == null) {
        		traceId = UUID.randomUUID().toString().replaceAll("-", "");
        		MDC.put(THIRFTJSOA_TRACE_KEY, traceId);
        	}
        	String msgName = traceId + SEPARATOR + tMessage.name;
        	
            super.writeMessageBegin(new TMessage(msgName, tMessage.type, tMessage.seqid));
        } else {
            super.writeMessageBegin(tMessage);
        }
    }
    
    public static class Factory implements TProtocolFactory {
		private static final long serialVersionUID = 1L;
		
		private final TProtocolFactory tProtocolFactory;

        public Factory(TProtocolFactory tProtocolFactory) {
        	this.tProtocolFactory = tProtocolFactory;
        }

        @Override
        public TProtocol getProtocol(TTransport trans) {
        	return new ThirftJsoaProtocol(tProtocolFactory.getProtocol(trans));
        }
    }
    
}