package com.halloffame.thriftjsoa.core.protocol;

import com.halloffame.thriftjsoa.core.base.TjProtocol;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;

import java.nio.ByteBuffer;

/**
 * Avro序列化协议 //todo
 * @author zhuwx
 */
public class TAvroProtocol extends TjProtocol {

    public static class Factory implements TProtocolFactory {
        private boolean directWriteReadObj;

        public Factory() {
            this(true);
        }
        public Factory(boolean directWriteReadObj) {
            this.directWriteReadObj = directWriteReadObj;
        }

        public TProtocol getProtocol(TTransport trans) {
            return new TAvroProtocol(trans, directWriteReadObj);
        }
    }

    /**
     * Constructor
     */
    public TAvroProtocol(TTransport trans) {
        this(trans, true);
    }
    public TAvroProtocol(TTransport trans, boolean directWriteReadObj) {
        super(trans, directWriteReadObj);
    }

    @Override
    public void writeDirectObjects(Object[] objs) {

    }

    @Override
    public Object[] readDirectObjects() { //Class<?>[] types
        return null;
    }

    @Override
    public void writeDirectObject(Object obj) {

    }

    @Override
    public Object readDirectObject() {
        return null;
    }

    /**
     * Writing methods.
     */
    @Override
    public void writeMessageBegin(TMessage message) throws TException {

    }

    @Override
    public void writeMessageEnd() throws TException {

    }

    @Override
    public void writeStructBegin(TStruct struct) throws TException {

    }

    @Override
    public void writeStructEnd() throws TException {

    }

    @Override
    public void writeFieldBegin(TField field) throws TException {

    }

    @Override
    public void writeFieldEnd() throws TException {

    }

    @Override
    public void writeFieldStop() throws TException {

    }

    @Override
    public void writeMapBegin(TMap map) throws TException {

    }

    @Override
    public void writeMapEnd() throws TException {

    }

    @Override
    public void writeListBegin(TList list) throws TException {

    }

    @Override
    public void writeListEnd() throws TException {

    }

    @Override
    public void writeSetBegin(TSet set) throws TException {

    }

    @Override
    public void writeSetEnd() throws TException {

    }

    @Override
    public void writeBool(boolean b) throws TException {

    }

    @Override
    public void writeByte(byte b) throws TException {

    }

    @Override
    public void writeI16(short i16) throws TException {

    }

    @Override
    public void writeI32(int i32) throws TException {

    }

    @Override
    public void writeI64(long i64) throws TException {

    }

    @Override
    public void writeDouble(double dub) throws TException {

    }

    @Override
    public void writeString(String str) throws TException {

    }

    @Override
    public void writeBinary(ByteBuffer buf) throws TException {

    }

    /**
     * Reading methods.
     */
    @Override
    public TMessage readMessageBegin() throws TException {
        return null;
    }

    @Override
    public void readMessageEnd() throws TException {

    }

    @Override
    public TStruct readStructBegin() throws TException {
        return null;
    }

    @Override
    public void readStructEnd() throws TException {

    }

    @Override
    public TField readFieldBegin() throws TException {
        return null;
    }

    @Override
    public void readFieldEnd() throws TException {

    }

    @Override
    public TMap readMapBegin() throws TException {
        return null;
    }

    @Override
    public void readMapEnd() throws TException {

    }

    @Override
    public TList readListBegin() throws TException {
        return null;
    }

    @Override
    public void readListEnd() throws TException {

    }

    @Override
    public TSet readSetBegin() throws TException {
        return null;
    }

    @Override
    public void readSetEnd() throws TException {

    }

    @Override
    public boolean readBool() throws TException {
        return false;
    }

    @Override
    public byte readByte() throws TException {
        return 0;
    }

    @Override
    public short readI16() throws TException {
        return 0;
    }

    @Override
    public int readI32() throws TException {
        return 0;
    }

    @Override
    public long readI64() throws TException {
        return 0;
    }

    @Override
    public double readDouble() throws TException {
        return 0;
    }

    @Override
    public String readString() throws TException {
        return null;
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        return null;
    }

}
