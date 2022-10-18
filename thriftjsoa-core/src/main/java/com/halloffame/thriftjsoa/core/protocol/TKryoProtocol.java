package com.halloffame.thriftjsoa.core.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import com.halloffame.thriftjsoa.core.base.TjProtocol;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.Getter;
import org.apache.thrift.TEnum;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TKryoTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Kryo序列化协议
 * @author zhuwx
 */
public class TKryoProtocol extends TjProtocol {
    //protected static final int VERSION_1 = 0x80010000;
    //protected static final int VERSION_MASK = 0xffff0000;

    public static final boolean KRYO_POOL_THREAD_SAFE = true;
    public static final boolean KRYO_POOL_SOFT_REFERENCES = false;
    public static final int KRYO_POOL_MAXIMUM_CAPACITY = 8;
    public static final int OUTPUT_BUFFER_SIZE = 1024;
    public static final int OUTPUT_MAX_BUFFER_SIZE = -1;

    protected boolean strictRead_;
    protected boolean strictWrite_;
    protected boolean optimizePositive;

    private TKryoTransport tKryoTransport = null;
    //private ThreadLocal<Output> outputTl = null;

    //private int outputBufferSize;
    //private int outputMaxBufferSize;
    //private boolean varEncoding;
    //private Output output = null;

    @Getter
    private final Pool<Kryo> kryoPool;
    private final Set<Class<?>> clazzs = new HashSet<>();

    public TKryoProtocol(TTransport trans, Set<Class<?>> classSet, boolean kryoPoolThreadSafe, boolean kryoPoolSoftReferences,
                         int kryoPoolMaximumCapacity, int outputBufferSize, int outputMaxBufferSize,
                         boolean strictRead, boolean strictWrite, boolean varEncoding, boolean optimizePositive, boolean directWriteReadObj) {
        super(trans, directWriteReadObj);
        if (trans instanceof TKryoTransport) {
            tKryoTransport = (TKryoTransport) trans;
        } else {
            tKryoTransport = new TKryoTransport((TSocket) trans, false);
        }
        tKryoTransport.getOutput().setVariableLengthEncoding(varEncoding);
        tKryoTransport.getInput().setVariableLengthEncoding(varEncoding);

        /* if (trans instanceof TKryoTransport) {
            tKryoTransport = (TKryoTransport) trans;
            tKryoTransport.getOutput().setVariableLengthEncoding(varEncoding);
            tKryoTransport.getInput().setVariableLengthEncoding(varEncoding);
        } else {
            outputTl = ThreadLocal.withInitial(() -> new Output(outputBufferSize, outputMaxBufferSize));
            outputTl = ThreadLocal.withInitial(() -> {
                Output output = new Output(outputBufferSize, outputMaxBufferSize);
                output.setVariableLengthEncoding(varEncoding);
                return output;
            });
        } */
        this.strictRead_ = strictRead;
        this.strictWrite_ = strictWrite;
        this.optimizePositive = optimizePositive;

        //this.outputBufferSize = outputBufferSize;
        //this.outputMaxBufferSize = outputMaxBufferSize;
        //this.varEncoding = varEncoding;

        if (classSet != null) {
            this.clazzs.addAll(classSet);
        }

        // Pool constructor arguments: thread safe, soft references, maximum capacity
        kryoPool = new Pool<Kryo>(kryoPoolThreadSafe, kryoPoolSoftReferences, kryoPoolMaximumCapacity) {
            protected Kryo create () {
                Kryo kryo = new Kryo();
                // Configure the Kryo instance.
                kryo.register(Object.class);
                kryo.register(Object[].class);
                kryo.register(Array.class);
                kryo.register(Enum.class);
                kryo.register(TEnum.class);

                kryo.register(TMessage.class);
                kryo.register(TStruct.class);
                kryo.register(TField.class);
                kryo.register(TMap.class);
                kryo.register(TList.class);
                kryo.register(TSet.class);

                kryo.register(Map.class);
                kryo.register(HashMap.class);
                kryo.register(TreeMap.class);
                kryo.register(Set.class);
                kryo.register(HashSet.class);
                kryo.register(TreeSet.class);
                kryo.register(List.class);
                kryo.register(ArrayList.class);
                kryo.register(LinkedList.class);

                kryo.register(BigDecimal.class);

                kryo.register(ThriftJsoaUtil.StructWrap.class);
                kryo.register(ThriftJsoaUtil.FieldWrap.class);

                for (Class<?> clazz : clazzs) {
                    kryo.register(clazz);
                }

                return kryo;
            }
        };
    }

    public TKryoProtocol(TTransport trans) {
        this(trans, false);
    }

    public TKryoProtocol(TTransport trans, boolean directWriteReadObj) {
        //this(trans, new Class[]{List.class}, directWriteReadObj);
        this(trans, null, directWriteReadObj);
    }

    public TKryoProtocol(TTransport trans, Set<Class<?>> classSet, boolean directWriteReadObj) {
        this(trans, classSet, KRYO_POOL_THREAD_SAFE, KRYO_POOL_SOFT_REFERENCES,
                KRYO_POOL_MAXIMUM_CAPACITY, OUTPUT_BUFFER_SIZE, OUTPUT_MAX_BUFFER_SIZE, false,
                true, true, false, directWriteReadObj);
    }

    public static class Factory implements TProtocolFactory {
        private final Set<Class<?>> clazzs;
        private final boolean kryoPoolThreadSafe;
        private final boolean kryoPoolSoftReferences;
        private final int kryoPoolMaximumCapacity;
        private final int outputBufferSize;
        private final int outputMaxBufferSize;

        protected boolean strictRead_;
        protected boolean strictWrite_;
        protected boolean varEncoding;
        protected boolean optimizePositive;

        private boolean directWriteReadObj;

        public Factory() {
            this(false);
        }

        public Factory(Set<Class<?>> classSet) {
            this(classSet, false);
        }

        public Factory(boolean directWriteReadObj) {
            this(null, directWriteReadObj);
        }

        public Factory(Set<Class<?>> classSet, boolean directWriteReadObj) {
            this(classSet, KRYO_POOL_THREAD_SAFE, KRYO_POOL_SOFT_REFERENCES, KRYO_POOL_MAXIMUM_CAPACITY, OUTPUT_BUFFER_SIZE,
                    OUTPUT_MAX_BUFFER_SIZE, false, true, true, false, directWriteReadObj);
        }

        public Factory(Set<Class<?>> classSet, boolean kryoPoolThreadSafe, boolean kryoPoolSoftReferences,
                       int kryoPoolMaximumCapacity, int outputBufferSize, int outputMaxBufferSize,
                       boolean strictRead, boolean strictWrite, boolean varEncoding, boolean optimizePositive, boolean directWriteReadObj) {
            this.clazzs = classSet;
            this.kryoPoolThreadSafe = kryoPoolThreadSafe;
            this.kryoPoolSoftReferences = kryoPoolSoftReferences;
            this.kryoPoolMaximumCapacity = kryoPoolMaximumCapacity;
            this.outputBufferSize = outputBufferSize;
            this.outputMaxBufferSize = outputMaxBufferSize;

            this.strictRead_ = strictRead;
            this.strictWrite_ = strictWrite;
            this.varEncoding = varEncoding;
            this.optimizePositive = optimizePositive;

            this.directWriteReadObj = directWriteReadObj;
        }

        public TProtocol getProtocol(TTransport trans) {
            return new TKryoProtocol(trans, clazzs, kryoPoolThreadSafe, kryoPoolSoftReferences, kryoPoolMaximumCapacity,
                    outputBufferSize, outputMaxBufferSize, strictRead_, strictWrite_, varEncoding, optimizePositive, directWriteReadObj);
        }

        public void addClazzs(Set<Class<?>> classSet) {
            if (classSet != null) {
                this.clazzs.addAll(classSet);
            }
        }
    }

    public void addClazzs(Set<Class<?>> classSet) {
        if (classSet != null) {
            this.clazzs.addAll(classSet);
        }
    }

    /* private Output getOutput() {
        //output = outputTl.get();
        if (output == null) {
            output = new Output(outputBufferSize, outputMaxBufferSize);
            output.setVariableLengthEncoding(varEncoding);
            //outputTl.set(output);
        } else {
            output.flush();
            output.reset();
        }
        return output;
    }

    public void closeOutput() {
        if (Objects.nonNull(outputTl)) {
            Output output = outputTl.get();
            if (output != null) {
                output.flush();
                output.close();
            }
            outputTl.remove();
        }
        if (output != null) {
            output.flush();
            output.close();
        }
    } */

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
        //closeOutput();
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
        tKryoTransport.getOutput().writeBoolean(b);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeBoolean(b);
        } else {
            Output output = getOutput();
            output.writeBoolean(b);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();

            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeByte(byte b) throws TException {
        tKryoTransport.getOutput().writeByte(b);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeByte(b);
        } else {
            Output output = getOutput();
            output.writeByte(b);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeI16(short i16) throws TException {
        tKryoTransport.getOutput().writeShort(i16);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeShort(i16);
        } else {
            Output output = getOutput();
            output.writeShort(i16);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeI32(int i32) throws TException {
        tKryoTransport.getOutput().writeInt(i32, optimizePositive);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeInt(i32, optimizePositive);
        } else {
            Output output = getOutput();
            output.writeInt(i32, optimizePositive);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeI64(long i64) throws TException {
        tKryoTransport.getOutput().writeLong(i64, optimizePositive);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeLong(i64, optimizePositive);
        } else {
            Output output = getOutput();
            output.writeLong(i64, optimizePositive);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeDouble(double dub) throws TException {
        tKryoTransport.getOutput().writeDouble(dub);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeDouble(dub);
        } else {
            Output output = getOutput();
            output.writeDouble(dub);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            trans_.write(bytes, 0, len);
        } */
    }

    @Override
    public void writeString(String str) throws TException {
        tKryoTransport.getOutput().writeString(str);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeString(str);
        } else {
            Output output = getOutput();
            output.writeString(str);
            output.flush();
            int len = output.position();
            byte[] bytes = output.getBuffer();
            output.reset();
            writeBinary(bytes, 0, len);
        } */
    }

    private void writeBinary(byte[] buf, int offset, int length) throws TException {
        writeI32(length);
        tKryoTransport.getOutput().writeBytes(buf, offset, length);
        /* if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getOutput().writeBytes(buf, 0, length);
        } else {
            trans_.write(buf, offset, length);
        } */
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
        return tKryoTransport.getInput().readByte();
    }

    @Override
    public short readI16() throws TException {
        return tKryoTransport.getInput().readShort();
    }

    @Override
    public int readI32() throws TException {
        return tKryoTransport.getInput().readInt();
    }

    @Override
    public long readI64() throws TException {
        return tKryoTransport.getInput().readLong();
    }

    @Override
    public double readDouble() throws TException {
        return tKryoTransport.getInput().readDouble();
    }

    @Override
    public String readString() throws TException {
        return tKryoTransport.getInput().readString();
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        int size = readI32();
        byte[] buf = new byte[size];
        tKryoTransport.getInput().readBytes(buf, 0, size);

        /*if (Objects.nonNull(tKryoTransport)) {
            tKryoTransport.getInput().readBytes(buf, 0, size);
        } else {
            trans_.readAll(buf, 0, size);
        } */

        return ByteBuffer.wrap(buf);
    }

    @Override
    public void writeDirectObjects(Object[] objs) {
        Kryo kryo = null;
        try {
            kryo = kryoPool.obtain();
            //kryo.writeObject(tKryoTransport.getOutput(), objs); //kryo.writeObjectOrNull(tKryoTransport.getOutput(), objs, Object[].class);
            kryo.writeClassAndObject(tKryoTransport.getOutput(), objs);

            /* if (Objects.nonNull(tKryoTransport)) {
                kryo.writeObject(tKryoTransport.getOutput(), obj); //kryo.writeClassAndObject(output, obj);
            } else {
                Output output = getOutput();
                kryo.writeObject(output, obj);
                output.flush();
                int len = output.position();
                byte[] bytes = output.getBuffer();
                output.reset();
                trans_.write(bytes, 0, len);
            } */
        } finally {
            if (Objects.nonNull(kryo)) {
                kryoPool.free(kryo);
            }
        }
    }

    @Override
    public Object[] readDirectObjects() { //Class<?>[] types
        Kryo kryo = null;
        try {
            kryo = kryoPool.obtain();
            return (Object[]) kryo.readClassAndObject(tKryoTransport.getInput());

            /* Object[] objs = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                objs[i] = kryo.readObjectOrNull(tKryoTransport.getInput(), types[i]);
            }
            return objs; */
        } finally {
            if (Objects.nonNull(kryo)) {
                kryoPool.free(kryo);
            }
        }
    }

    @Override
    public void writeDirectObject(Object obj) {
        Kryo kryo = null;
        try {
            kryo = kryoPool.obtain();
            kryo.writeClassAndObject(tKryoTransport.getOutput(), obj);
        } finally {
            if (Objects.nonNull(kryo)) {
                kryoPool.free(kryo);
            }
        }
    }

    @Override
    public Object readDirectObject() {
        Kryo kryo = null;
        try {
            kryo = kryoPool.obtain();
            return kryo.readClassAndObject(tKryoTransport.getInput());
        } finally {
            if (Objects.nonNull(kryo)) {
                kryoPool.free(kryo);
            }
        }
    }

}
