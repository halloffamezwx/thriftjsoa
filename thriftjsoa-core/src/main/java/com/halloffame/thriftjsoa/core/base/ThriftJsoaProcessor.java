package com.halloffame.thriftjsoa.core.base;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * ThriftJsoa的Processor，用于消息头的traceId和appId的解析处理 以及 链接的连通有效性检查
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaProcessor implements TProcessor {

    /**
     * 封装的TProcessor
     */
    private final TProcessor tProcessor;

    /**
     * 链接的连通有效性检查的请求的不存在的接口名
     */
    private final String connValidateMethodName;

    /**
     * 优雅关机的请求的不存在的接口名
     */
    private final String shutdownGracefulMethodName;

    /**
     * 获取服务状态的请求的不存在的接口名
     */
    private final String getServerStatusMethodName;
    
    public ThriftJsoaProcessor(TProcessor tProcessor, String connValidateMethodName, String shutdownGracefulMethodName, String getServerStatusMethodName) {
    	this.tProcessor = tProcessor;
    	this.connValidateMethodName = connValidateMethodName;
    	this.shutdownGracefulMethodName = shutdownGracefulMethodName;
    	this.getServerStatusMethodName = getServerStatusMethodName;
    }

    @Override
    public boolean process(TProtocol iprot, TProtocol oprot) throws TException {
        TMessage message = iprot.readMessageBegin();
        if (message.type != TMessageType.CALL && message.type != TMessageType.ONEWAY) {
            throw new TException("This should not have happened!?");
        }

        //消息头的组成是：traceId + "," + appId + "," + 原始值
        String[] msgNameArr = message.name.split(ThriftJsoaProtocol.FIELD_SEPARATOR);
        String appId = null;
        String traceId;
        String msgName;

        if (msgNameArr.length != ThriftJsoaProtocol.FIELD_SIZE) {
            traceId = ThriftJsoaUtil.genStrId();
            msgName = message.name;
        } else {
            traceId = msgNameArr[ThriftJsoaProtocol.TRACE_ID_FIELD_INDEX];
            appId = msgNameArr[ThriftJsoaProtocol.APP_ID_FIELD_INDEX];

            if (traceId == null || "".equals(traceId.trim()) || "null".equalsIgnoreCase(traceId.trim())) {
                traceId = ThriftJsoaUtil.genStrId();
            }

            msgName = msgNameArr[ThriftJsoaProtocol.ORIGINAL_FIELD_INDEX];
        }
        //把消息头的traceId和appId存放到MDC，这样任何地方的业务代码有需要的话就可以从MDC取出来
        MDC.put(ThriftJsoaProtocol.TRACE_KEY, traceId);
        MDC.put(ThriftJsoaProtocol.APP_KEY, appId);

        //链接的连通有效性检查，直接返回TApplicationException.UNKNOWN_METHOD
        if (connValidateMethodName.equals(msgName)) {
            TProtocolUtil.skip(iprot, TType.STRUCT);
            iprot.readMessageEnd();

            TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + message.name + "'");
            oprot.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
            x.write(oprot);
            oprot.writeMessageEnd();
            oprot.getTransport().flush();

            return true;
        } else if (shutdownGracefulMethodName.equals(msgName)) {
            TProtocolUtil.skip(iprot, TType.STRUCT);
            iprot.readMessageEnd();
            try {
                if (!CommonServer.getGracefulShutdownThread().isAlive()) {
                    CommonServer.getGracefulShutdownThread().start();
                }
            } catch (Exception e) {
                log.error("", e);
                TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
                oprot.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
                x.write(oprot);
                oprot.writeMessageEnd();
                oprot.getTransport().flush();
                return true;
            }

            oprot.writeMessageBegin(new TMessage(message.name, TMessageType.REPLY, message.seqid));
            oprot.writeStructBegin(new TStruct(""));
            oprot.writeFieldStop();
            oprot.writeStructEnd();
            oprot.writeMessageEnd();
            oprot.getTransport().flush();
            return true;
        } else if (getServerStatusMethodName.equals(msgName)) {
        }

        TMessage standardMessage = new TMessage(msgName, message.type, message.seqid);
        InStoredMessageProtocol inStoredMessageProtocol = new InStoredMessageProtocol(iprot, standardMessage);
        OutStoredMessageProtocol outStoredMessageProtocol = new OutStoredMessageProtocol(oprot, standardMessage);

        inStoredMessageProtocol.log();
        try {
            return tProcessor.process(inStoredMessageProtocol, outStoredMessageProtocol);
        } finally {
            outStoredMessageProtocol.log();
        }
    }

    /**
     * TProtocol已经从流里面读取了消息头并处理过，需要重新装饰，让后面的流程继续读取正确的消息头
     */
    private static class InStoredMessageProtocol extends TProtocolDecorator {

        /**
         * 封装的消息头
         */
        TMessage messageBegin;

        /**
         * 请求体数据
         */
        List<Object> inValueData;

        int i = 0;

        boolean isLogMethod = false;

        TProtocol protocol;

        public InStoredMessageProtocol(TProtocol protocol, TMessage messageBegin) {
            super(protocol);
            this.protocol = protocol;
            this.messageBegin = messageBegin;

            String logInMethod = System.getProperty("thriftjsoa.processor.log.in.method");
            if (logInMethod != null && logInMethod.contains(messageBegin.name)) {
                isLogMethod = true;
            }
        }

        public void log() throws TException {
            if (isLogMethod) {
                if (inValueData == null) {
                    inValueData = new ArrayList<>();
                    inValueData.add(messageBegin);
                    inValueData.addAll(ThriftJsoaUtil.readValueData(protocol));
                    protocol.readMessageEnd();
                }
                log.info("in: " + inValueData);
            }
        }

        private Object getOneValueData() {
            if (i >= inValueData.size()) {
                i = 0;
            }
            return inValueData.get(i++);
        }

        /**
         * read method
         */
        @Override
        public TMessage readMessageBegin() throws TException {
            if (isLogMethod) {
                return (TMessage) getOneValueData();
            } else {
                return messageBegin;
            }
        }

        @Override
        public void readMessageEnd() throws TException {
            if (!isLogMethod) {
                super.readMessageEnd();
            }
        }

        @Override
        public TStruct readStructBegin() throws TException {
            if (isLogMethod) {
                return (TStruct) getOneValueData();
            } else {
                return super.readStructBegin();
            }
        }

        @Override
        public void readStructEnd() throws TException {
            if (!isLogMethod) {
                super.readStructEnd();
            }
        }

        @Override
        public TField readFieldBegin() throws TException {
            if (isLogMethod) {
                return (TField) getOneValueData();
            } else {
                return super.readFieldBegin();
            }
        }

        @Override
        public void readFieldEnd() throws TException {
            if (!isLogMethod) {
                super.readFieldEnd();
            }
        }

        @Override
        public TMap readMapBegin() throws TException {
            if (isLogMethod) {
                return (TMap) getOneValueData();
            } else {
                return super.readMapBegin();
            }
        }

        @Override
        public void readMapEnd() throws TException {
            if (!isLogMethod) {
                super.readMapEnd();
            }
        }

        @Override
        public TList readListBegin() throws TException {
            if (isLogMethod) {
                return (TList) getOneValueData();
            } else {
                return super.readListBegin();
            }
        }

        @Override
        public void readListEnd() throws TException {
            if (!isLogMethod) {
                super.readListEnd();
            }
        }

        @Override
        public TSet readSetBegin() throws TException {
            if (isLogMethod) {
                return (TSet) getOneValueData();
            } else {
                return super.readSetBegin();
            }
        }

        @Override
        public void readSetEnd() throws TException {
            if (!isLogMethod) {
                super.readSetEnd();
            }
        }

        @Override
        public boolean readBool() throws TException {
            if (isLogMethod) {
                return (boolean) getOneValueData();
            } else {
                return super.readBool();
            }
        }

        @Override
        public byte readByte() throws TException {
            if (isLogMethod) {
                return (byte) getOneValueData();
            } else {
                return super.readByte();
            }
        }

        @Override
        public short readI16() throws TException {
            if (isLogMethod) {
                return (short) getOneValueData();
            } else {
                return super.readI16();
            }
        }

        @Override
        public int readI32() throws TException {
            if (isLogMethod) {
                return (int) getOneValueData();
            } else {
                return super.readI32();
            }
        }

        @Override
        public long readI64() throws TException {
            if (isLogMethod) {
                return (long) getOneValueData();
            } else {
                return super.readI64();
            }
        }

        @Override
        public double readDouble() throws TException {
            if (isLogMethod) {
                return (double) getOneValueData();
            } else {
                return super.readDouble();
            }
        }

        @Override
        public String readString() throws TException {
            if (isLogMethod) {
                return (String) getOneValueData();
            } else {
                return super.readString();
            }
        }

        @Override
        public ByteBuffer readBinary() throws TException {
            if (isLogMethod) {
                return (ByteBuffer) getOneValueData();
            } else {
                return super.readBinary();
            }
        }

    }

    /**
     * 用于缓存响应体数据并打印出参日志
     */
    private static class OutStoredMessageProtocol extends TProtocolDecorator {

        /**
         * 响应体数据
         */
        List<Object> outValueData;

        boolean isLogMethod = false;

        TProtocol protocol;

        public OutStoredMessageProtocol(TProtocol protocol, TMessage messageBegin) {
            super(protocol);
            this.protocol = protocol;

            String logOutMethod = System.getProperty("thriftjsoa.processor.log.out.method");
            if (logOutMethod != null && logOutMethod.contains(messageBegin.name)) {
                isLogMethod = true;
            }
        }

        public void log() throws TException {
            if (isLogMethod) {
                log.info("out: " + outValueData);
                ThriftJsoaUtil.writeValueData(protocol, outValueData);
            }
        }

        /**
         * write method
         */
        @Override
        public void writeMessageBegin(TMessage tMessage) throws TException {
            if (isLogMethod) {
                outValueData.add(tMessage);
            } else {
                super.writeMessageBegin(tMessage);
            }
        }

        @Override
        public void writeMessageEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.MessageEnd());
            } else {
                super.writeMessageEnd();
            }
        }

        @Override
        public void writeStructBegin(TStruct tStruct) throws TException {
            if (isLogMethod) {
                outValueData.add(tStruct);
            } else {
                super.writeStructBegin(tStruct);
            }
        }

        @Override
        public void writeStructEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.StructEnd());
            } else {
                super.writeStructEnd();
            }
        }

        @Override
        public void writeFieldBegin(TField tField) throws TException {
            if (isLogMethod) {
                outValueData.add(tField);
            } else {
                super.writeFieldBegin(tField);
            }
        }

        @Override
        public void writeFieldEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.FieldEnd());
            } else {
                super.writeFieldEnd();
            }
        }

        @Override
        public void writeFieldStop() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.FieldStop());
            } else {
                super.writeFieldStop();
            }
        }

        @Override
        public void writeMapBegin(TMap tMap) throws TException {
            if (isLogMethod) {
                outValueData.add(tMap);
            } else {
                super.writeMapBegin(tMap);
            }
        }

        @Override
        public void writeMapEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.MapEnd());
            } else {
                super.writeMapEnd();
            }
        }

        @Override
        public void writeListBegin(TList tList) throws TException {
            if (isLogMethod) {
                outValueData.add(tList);
            } else {
                super.writeListBegin(tList);
            }
        }

        @Override
        public void writeListEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.ListEnd());
            } else {
                super.writeListEnd();
            }
        }

        @Override
        public void writeSetBegin(TSet tSet) throws TException {
            if (isLogMethod) {
                outValueData.add(tSet);
            } else {
                super.writeSetBegin(tSet);
            }
        }

        @Override
        public void writeSetEnd() throws TException {
            if (isLogMethod) {
                outValueData.add(new ThriftJsoaUtil.SetEnd());
            } else {
                super.writeSetEnd();
            }
        }

        @Override
        public void writeBool(boolean b) throws TException {
            if (isLogMethod) {
                outValueData.add(b);
            } else {
                super.writeBool(b);
            }
        }

        @Override
        public void writeByte(byte b) throws TException {
            if (isLogMethod) {
                outValueData.add(b);
            } else {
                super.writeByte(b);
            }
        }

        @Override
        public void writeI16(short i) throws TException {
            if (isLogMethod) {
                outValueData.add(i);
            } else {
                super.writeI16(i);
            }
        }

        @Override
        public void writeI32(int i) throws TException {
            if (isLogMethod) {
                outValueData.add(i);
            } else {
                super.writeI32(i);
            }
        }

        @Override
        public void writeI64(long l) throws TException {
            if (isLogMethod) {
                outValueData.add(l);
            } else {
                super.writeI64(l);
            }
        }

        @Override
        public void writeDouble(double v) throws TException {
            if (isLogMethod) {
                outValueData.add(v);
            } else {
                super.writeDouble(v);
            }
        }

        @Override
        public void writeString(String s) throws TException {
            if (isLogMethod) {
                outValueData.add(s);
            } else {
                super.writeString(s);
            }
        }

        @Override
        public void writeBinary(ByteBuffer buf) throws TException {
            if (isLogMethod) {
                outValueData.add(buf);
            } else {
                super.writeBinary(buf);
            }
        }
    }

}
