package com.halloffame.thriftjsoa.core.base;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;

/**
 * Tj的MultiplexedProtocol
 * @author zhuwx
 */
public class TjMultiplexedProtocol extends TjProtocolDecorator {

    public static final String SEPARATOR = ":";

    private final String SERVICE_NAME;

    public TjMultiplexedProtocol(TProtocol protocol, String serviceName) {
        super(protocol);
        SERVICE_NAME = serviceName;
    }

    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {
        if (tMessage.type == TMessageType.CALL || tMessage.type == TMessageType.ONEWAY) {
            super.writeMessageBegin(new TMessage(
                    SERVICE_NAME + SEPARATOR + tMessage.name,
                    tMessage.type,
                    tMessage.seqid
            ));
        } else {
            super.writeMessageBegin(tMessage);
        }
    }
}
