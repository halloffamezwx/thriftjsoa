package com.halloffame.thriftjsoa.core.protocol;

import com.halloffame.thriftjsoa.core.base.TjProtocol;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.SneakyThrows;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TSocketAdapterTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.nio.ByteBuffer;

public class TProtostuffProtocol extends TjProtocol {

    private TSocketAdapterTransport tSocketAdapterTransport;
    private LinkedBuffer buffer = LinkedBuffer.allocate(512);

    public static final Schema<Short> SHORT_SCHEMA = RuntimeSchema.getSchema(Short.class);
    public static final Schema<Integer> INTEGER_SCHEMA = RuntimeSchema.getSchema(Integer.class);
    public static final Schema<Long> LONG_SCHEMA = RuntimeSchema.getSchema(Long.class);
    public static final Schema<Double> DOUBLE_SCHEMA = RuntimeSchema.getSchema(Double.class);
    public static final Schema<Boolean> BOOLEAN_SCHEMA = RuntimeSchema.getSchema(Boolean.class);
    public static final Schema<Byte> BYTE_SCHEMA = RuntimeSchema.getSchema(Byte.class);
    public static final Schema<String> STRING_SCHEMA = RuntimeSchema.getSchema(String.class);
    public static final Schema<Byte[]> BYTE_ARRAY_SCHEMA = RuntimeSchema.getSchema(Byte[].class);
    public static final Schema<byte[]> B_ARRAY_SCHEMA = RuntimeSchema.getSchema(byte[].class);
    public static final Schema<Object> OBJECT_SCHEMA = RuntimeSchema.getSchema(Object.class);
    public static final Schema<Object[]> OBJECTS_SCHEMA = RuntimeSchema.getSchema(Object[].class);

    public static class Factory implements TProtocolFactory {
        private boolean directWriteReadObj;

        public Factory() {
            this(false);
        }
        private Factory(boolean directWriteReadObj) {
            this.directWriteReadObj = directWriteReadObj;
        }

        public TProtocol getProtocol(TTransport trans) {
            return new TProtostuffProtocol(trans, directWriteReadObj);
        }
    }

    /**
     * Constructor
     */
    public TProtostuffProtocol(TTransport trans) {
        this(trans, false);
    }
    private TProtostuffProtocol(TTransport trans, boolean directWriteReadObj) {
        super(trans, directWriteReadObj);
        if (trans instanceof TSocketAdapterTransport) {
            tSocketAdapterTransport = (TSocketAdapterTransport) trans;
        } else {
            tSocketAdapterTransport = new TSocketAdapterTransport((TSocket) trans);
        }
    }

    @SneakyThrows
    @Override
    public void writeDirectObjects(Object[] objs) {
        buffer = buffer.clear();
        ProtostuffIOUtil.writeTo(tSocketAdapterTransport.getOutputStream(), objs, OBJECTS_SCHEMA, buffer);
    }

    @SneakyThrows
    @Override
    public Object[] readDirectObjects() { //Class<?>[] types

        Object[] objs = OBJECTS_SCHEMA.newMessage();
        ProtostuffIOUtil.mergeFrom(tSocketAdapterTransport.getInputStream(), objs, OBJECTS_SCHEMA);
        return objs;

        /* Object[] objs = new Object[types.length];

        for (int i = 0; i < types.length; i++) {
            //Schema<?> schema = RuntimeSchema.getSchema(types[i]);
            objs[i] = OBJECT_SCHEMA.newMessage();
            ProtostuffIOUtil.mergeFrom(tSocketAdapterTransport.getInputStream(), objs[i], OBJECT_SCHEMA);
        }

        return objs; */
    }

    @SneakyThrows
    @Override
    public void writeDirectObject(Object obj) {
        buffer = buffer.clear();
        ProtostuffIOUtil.writeTo(tSocketAdapterTransport.getOutputStream(), obj, OBJECT_SCHEMA, buffer);
    }

    @SneakyThrows
    @Override
    public Object readDirectObject() {
        Object obj = OBJECT_SCHEMA.newMessage();
        ProtostuffIOUtil.mergeFrom(tSocketAdapterTransport.getInputStream(), obj, OBJECT_SCHEMA);
        return obj;
    }

    @SneakyThrows
    private <T> void writeSchemaObject(T t, Schema<T> schema) {
        buffer = buffer.clear();
        ProtostuffIOUtil.writeTo(tSocketAdapterTransport.getOutputStream(), t, schema, buffer);
    }

    @SneakyThrows
    private <T> T readSchemaObject(Schema<T> schema) {
        T t = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(tSocketAdapterTransport.getInputStream(), t, schema);
        return t;
    }

    /**
     * Writing methods.
     */
    @Override
    public void writeMessageBegin(TMessage message) throws TException {
        writeString(message.name);
        writeByte(message.type);
        writeI32(message.seqid);
        //writeObject(message);
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
        writeByte(field.type);
        writeI16(field.id);
        //writeObject(field);
    }

    @Override
    public void writeFieldEnd() throws TException {

    }

    @Override
    public void writeFieldStop() throws TException {
        writeByte(TType.STOP);
    }

    @Override
    public void writeMapBegin(TMap map) throws TException {
        writeByte(map.keyType);
        writeByte(map.valueType);
        writeI32(map.size);
        //writeObject(map);
    }

    @Override
    public void writeMapEnd() throws TException {

    }

    @Override
    public void writeListBegin(TList list) throws TException {
        writeByte(list.elemType);
        writeI32(list.size);
        //writeObject(list);
    }

    @Override
    public void writeListEnd() throws TException {

    }

    @Override
    public void writeSetBegin(TSet set) throws TException {
        writeByte(set.elemType);
        writeI32(set.size);
        //writeObject(set);
    }

    @Override
    public void writeSetEnd() throws TException {

    }

    @Override
    public void writeBool(boolean b) throws TException {
        writeSchemaObject(b, BOOLEAN_SCHEMA);
    }

    @Override
    public void writeByte(byte b) throws TException {
        writeSchemaObject(b, BYTE_SCHEMA);
    }

    @Override
    public void writeI16(short i16) throws TException {
        writeSchemaObject(i16, SHORT_SCHEMA);
    }

    @Override
    public void writeI32(int i32) throws TException {
        writeSchemaObject(i32, INTEGER_SCHEMA);
    }

    @Override
    public void writeI64(long i64) throws TException {
        writeSchemaObject(i64, LONG_SCHEMA);
    }

    @Override
    public void writeDouble(double dub) throws TException {
        writeSchemaObject(dub, DOUBLE_SCHEMA);
    }

    @Override
    public void writeString(String str) throws TException {
        writeSchemaObject(str, STRING_SCHEMA);
    }

    private void writeBinary(byte[] buf, int offset, int length) throws TException {
        //writeI32(length);
        //int j = length + offset;
        //for (int i = offset; i < j; i++) {
        //    writeObject(buf[i], BYTE_SCHEMA);
        //}
        byte[] writeBuf = new byte[length];
        System.arraycopy(buf, offset, writeBuf, 0, length);
        writeSchemaObject(writeBuf, B_ARRAY_SCHEMA);
    }

    @Override
    public void writeBinary(ByteBuffer buf) throws TException {
        int length = buf.limit() - buf.position();
        writeBinary(buf.array(), buf.position() + buf.arrayOffset(), length);
    }

    /**
     * Reading methods.
     */
    @Override
    public TMessage readMessageBegin() throws TException {
        return new TMessage(readString(), readByte(), readI32());
        //return readObject(TMessage.class);
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
        byte type = readByte();
        short id = type == TType.STOP ? 0 : readI16();
        return new TField("", type, id);
        //return readObject(TField.class);
    }

    @Override
    public void readFieldEnd() throws TException {

    }

    @Override
    public TMap readMapBegin() throws TException {
        return new TMap(readByte(), readByte(), readI32());
        //return readObject(TMap.class);
    }

    @Override
    public void readMapEnd() throws TException {

    }

    @Override
    public TList readListBegin() throws TException {
        return new TList(readByte(), readI32());
        //return readObject(TList.class);
    }

    @Override
    public void readListEnd() throws TException {

    }

    @Override
    public TSet readSetBegin() throws TException {
        return new TSet(readByte(), readI32());
        //return readObject(TSet.class);
    }

    @Override
    public void readSetEnd() throws TException {

    }

    @Override
    public boolean readBool() throws TException {
        return (readByte() == 1);
    }

    @Override
    public byte readByte() throws TException {
        return readSchemaObject(BYTE_SCHEMA);
    }

    @Override
    public short readI16() throws TException {
        return readSchemaObject(SHORT_SCHEMA);
    }

    @Override
    public int readI32() throws TException {
        return readSchemaObject(INTEGER_SCHEMA);
    }

    @Override
    public long readI64() throws TException {
        return readSchemaObject(LONG_SCHEMA);
    }

    @Override
    public double readDouble() throws TException {
        return readSchemaObject(DOUBLE_SCHEMA);
    }

    @Override
    public String readString() throws TException {
        return readSchemaObject(STRING_SCHEMA);
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        //int size = readI32();
        //byte[] buf = new byte[size];
        //for (int i = 0; i < size; i++) {
        //    buf[i] = readObject(BYTE_SCHEMA);
        //}
        byte[] buf = readSchemaObject(B_ARRAY_SCHEMA);
        return ByteBuffer.wrap(buf);
    }
}
