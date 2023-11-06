package com.halloffame.thriftjsoa.core.protocol;

import com.halloffame.thriftjsoa.core.base.TjProtocol;
import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSocketAdapterTransport;
import org.apache.thrift.transport.TTransport;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Avro序列化协议
 * @author zhuwx
 */
//@Deprecated
public class TAvroProtocol extends TjProtocol {

    private final TSocketAdapterTransport tSocketAdapterTransport;
    private final Decoder in;
    private final Encoder out;

    public static final DatumReader<Short> SHORT_READER = new GenericDatumReader<>(Schema.create(Schema.Type.INT));
    public static final DatumReader<Integer> INTEGER_READER = new GenericDatumReader<>(Schema.create(Schema.Type.INT));
    public static final DatumReader<Long> LONG_READER = new GenericDatumReader<>(Schema.create(Schema.Type.LONG));
    public static final DatumReader<Double> DOUBLE_READER = new GenericDatumReader<>(Schema.create(Schema.Type.DOUBLE));
    public static final DatumReader<Boolean> BOOLEAN_READER = new GenericDatumReader<>(Schema.create(Schema.Type.BOOLEAN));
    public static final DatumReader<Byte> BYTE_READER = new GenericDatumReader<>(Schema.create(Schema.Type.BYTES));
    public static final DatumReader<String> STRING_READER = new GenericDatumReader<>(Schema.create(Schema.Type.STRING));
    public static final DatumReader<Byte[]> BYTE_ARRAY_READER = new GenericDatumReader<>(Schema.create(Schema.Type.BYTES));
    public static final DatumReader<byte[]> B_ARRAY_READER = new GenericDatumReader<>(Schema.create(Schema.Type.BYTES));
    public static final DatumReader<Object> OBJECT_READER = new GenericDatumReader<>(Schema.create(Schema.Type.RECORD));
    public static final DatumReader<Object[]> OBJECTS_READER = new GenericDatumReader<>(Schema.create(Schema.Type.ARRAY));

    public static final DatumWriter<Short> SHORT_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.INT));
    public static final DatumWriter<Integer> INTEGER_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.INT));
    public static final DatumWriter<Long> LONG_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.LONG));
    public static final DatumWriter<Double> DOUBLE_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.DOUBLE));
    public static final DatumWriter<Boolean> BOOLEAN_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.BOOLEAN));
    public static final DatumWriter<Byte> BYTE_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.BYTES));
    public static final DatumWriter<String> STRING_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.STRING));
    public static final DatumWriter<Byte[]> BYTE_ARRAY_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.BYTES));
    public static final DatumWriter<byte[]> B_ARRAY_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.BYTES));
    public static final DatumWriter<Object> OBJECT_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.RECORD));
    public static final DatumWriter<Object[]> OBJECTS_WRITER = new GenericDatumWriter<>(Schema.create(Schema.Type.ARRAY));

    public static class Factory implements TProtocolFactory {
        private final boolean directWriteReadObj;

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
        if (trans instanceof TSocketAdapterTransport) {
            tSocketAdapterTransport = (TSocketAdapterTransport) trans;
        } else {
            tSocketAdapterTransport = new TSocketAdapterTransport((TSocket) trans);
        }
        in = DecoderFactory.get().binaryDecoder(tSocketAdapterTransport.getInputStream(), null);
        out = EncoderFactory.get().binaryEncoder(tSocketAdapterTransport.getOutputStream(), null);
    }

    @SneakyThrows
    @Override
    public void writeDirectObjects(Object[] objs) {
        new ReflectDatumWriter<>().write(objs, out);
    }

    @SneakyThrows
    @Override
    public Object[] readDirectObjects() { //Class<?>[] types
        return new ReflectDatumReader<Object[]>().read(null, in);
    }

    @SneakyThrows
    @Override
    public void writeDirectObject(Object obj) {
        new ReflectDatumWriter<>().write(obj, out);
    }

    @SneakyThrows
    @Override
    public Object readDirectObject() {
        return new ReflectDatumReader<>().read(null, in);
    }

    /**
     * Writing methods.
     */
    @Override
    public void writeMessageBegin(TMessage message) throws TException {
        writeString(message.name);
        writeByte(message.type);
        writeI32(message.seqid);
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
    }

    @Override
    public void writeMapEnd() throws TException {

    }

    @Override
    public void writeListBegin(TList list) throws TException {
        writeByte(list.elemType);
        writeI32(list.size);
    }

    @Override
    public void writeListEnd() throws TException {

    }

    @Override
    public void writeSetBegin(TSet set) throws TException {
        writeByte(set.elemType);
        writeI32(set.size);
    }

    @Override
    public void writeSetEnd() throws TException {

    }

    @SneakyThrows
    @Override
    public void writeBool(boolean b) throws TException {
        BOOLEAN_WRITER.write(b, out);
    }

    @SneakyThrows
    @Override
    public void writeByte(byte b) throws TException {
        BYTE_WRITER.write(b, out);
    }

    @SneakyThrows
    @Override
    public void writeI16(short i16) throws TException {
        SHORT_WRITER.write(i16, out);
    }

    @SneakyThrows
    @Override
    public void writeI32(int i32) throws TException {
        INTEGER_WRITER.write(i32, out);
    }

    @SneakyThrows
    @Override
    public void writeI64(long i64) throws TException {
        LONG_WRITER.write(i64, out);
    }

    @SneakyThrows
    @Override
    public void writeDouble(double dub) throws TException {
        DOUBLE_WRITER.write(dub, out);
    }

    @SneakyThrows
    @Override
    public void writeString(String str) throws TException {
        STRING_WRITER.write(str, out);
    }

    @SneakyThrows
    private void writeBinary(byte[] buf, int offset, int length) throws TException {
        byte[] writeBuf = new byte[length];
        System.arraycopy(buf, offset, writeBuf, 0, length);
        B_ARRAY_WRITER.write(writeBuf, out);
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
    }

    @Override
    public void readFieldEnd() throws TException {

    }

    @Override
    public TMap readMapBegin() throws TException {
        return new TMap(readByte(), readByte(), readI32());
    }

    @Override
    public void readMapEnd() throws TException {

    }

    @Override
    public TList readListBegin() throws TException {
        return new TList(readByte(), readI32());
    }

    @Override
    public void readListEnd() throws TException {

    }

    @Override
    public TSet readSetBegin() throws TException {
        return new TSet(readByte(), readI32());
    }

    @Override
    public void readSetEnd() throws TException {

    }

    @SneakyThrows
    @Override
    public boolean readBool() throws TException {
        return BOOLEAN_READER.read(false, in);
    }

    @SneakyThrows
    @Override
    public byte readByte() throws TException {
        return BYTE_READER.read((byte) 0, in);
    }

    @SneakyThrows
    @Override
    public short readI16() throws TException {
        return INTEGER_READER.read(0, in).shortValue();
    }

    @SneakyThrows
    @Override
    public int readI32() throws TException {
        return INTEGER_READER.read(0, in);
    }

    @SneakyThrows
    @Override
    public long readI64() throws TException {
        return LONG_READER.read(0L, in);
    }

    @SneakyThrows
    @Override
    public double readDouble() throws TException {
        return DOUBLE_READER.read(0.0, in);
    }

    @SneakyThrows
    @Override
    public String readString() throws TException {
        return STRING_READER.read(null, in);
    }

    @SneakyThrows
    @Override
    public ByteBuffer readBinary() throws TException {
        byte[] buf = B_ARRAY_READER.read(null, in);
        return ByteBuffer.wrap(buf);
    }

    /* static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach(logger -> {
            logger.setLevel(Level.INFO);
        });
    } */

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema.Parser().parse(TAvroProtocol.class.getClassLoader().getResource("user.avsc").openConnection().getInputStream());
        System.out.println("1=>" + schema.toString());
        //System.out.println(schema.getClass());

        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("name", "Alyssa");
        user1.put("favorite_number", 256);

        GenericRecord user2 = new GenericData.Record(schema);
        user2.put("name", "Ben");
        user2.put("favorite_number", 7);
        user2.put("favorite_color", "red");

        File file = new File("users.avro");
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
        dataFileWriter.create(schema, file);
        dataFileWriter.append(user1);
        dataFileWriter.append(user2);
        dataFileWriter.close();

        List<Schema.Field> fields = new ArrayList<>();
        Schema.Field field1 = new Schema.Field("name", Schema.create(Schema.Type.STRING));
        fields.add(field1);
        Schema.Field field2 = new Schema.Field("favorite_number", Schema.createUnion(Schema.create(Schema.Type.INT), Schema.create(Schema.Type.NULL)));
        fields.add(field2);
        Schema.Field field3 = new Schema.Field("favorite_color", Schema.createUnion(Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL)));
        fields.add(field3);

        Schema schema1 = Schema.createRecord("User", null, "example.avro", false, fields);
        System.out.println("2=>" +schema1.toString());

        // Deserialize users from disk
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema1);
        DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(file, datumReader);
        GenericRecord user = null;
        while (dataFileReader.hasNext()) {
            user = dataFileReader.next(user);
            System.out.println(user);
        }

        new ReflectDatumReader<>(user1.getClass());
        new SpecificDatumReader<>(user1.getClass());
        new ReflectDatumWriter<>(user1.getClass());
        new SpecificDatumWriter<>(user1.getClass());
    }

}
