package com.halloffame.thriftjsoa.base;

import com.halloffame.thriftjsoa.common.CommonServer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.slf4j.MDC;

/**
 * ThriftJsoa的Protocol，用于消息头的traceId和appId的解析处理等
 * @author zhuwx
 */
public class ThriftJsoaProtocol extends TProtocolDecorator {

    /**
     * 消息头name参数里面字段的分隔符
     */
    public static final String FIELD_SEPARATOR = ",";

    /**
     * 消息头name参数里面字段的个数
     */
    public static final int FIELD_SIZE = 3;

    /**
     * 消息头name参数里面调用链跟踪id字段的位置
     */
    public static final int TRACE_ID_FIELD_INDEX = 0;

    /**
     * 消息头name参数里面调用者id字段的位置
     */
    public static final int APP_ID_FIELD_INDEX = 1;

    /**
     * 消息头name参数里面原始字段的位置
     */
    public static final int ORIGINAL_FIELD_INDEX = 2;

    /**
     * 调用链跟踪id的key
     */
    public static final String TRACE_KEY = "thriftjsoa_trace_id";

    /**
     * 调用者id的key
     */
    public static final String APP_KEY = "thriftjsoa_app_id";

    public ThriftJsoaProtocol(TProtocol protocol) {
        super(protocol);
    }

    /**
     * 写消息头
     */
    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {

        String traceId = MDC.get(TRACE_KEY);

        StringBuilder msgNameBuilder = new StringBuilder();
        msgNameBuilder.append(traceId).append(FIELD_SEPARATOR).append(CommonServer.appId).append(FIELD_SEPARATOR).append(tMessage.name);

        super.writeMessageBegin(new TMessage(msgNameBuilder.toString(), tMessage.type, tMessage.seqid));
    }

    /**
     * ThriftJsoaProtocol的工厂类
     */
    public static class Factory implements TProtocolFactory {

		private static final long serialVersionUID = 1L;

        /**
         * 封装的TProtocolFactory
         */
		private final TProtocolFactory tProtocolFactory;

        public Factory(TProtocolFactory tProtocolFactory) {
        	this.tProtocolFactory = tProtocolFactory;
        }

        @Override
        public TProtocol getProtocol(TTransport trans) {
        	return new ThriftJsoaProtocol(tProtocolFactory.getProtocol(trans));
        }
    }
    
}
