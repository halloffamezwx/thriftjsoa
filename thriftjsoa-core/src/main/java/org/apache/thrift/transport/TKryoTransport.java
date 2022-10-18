package org.apache.thrift.transport;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import lombok.Getter;

/**
 * Kryo传输
 * @author zhuwx
 */
public class TKryoTransport extends TTransport {

    public final static int IN_BUFFER_SIZE = 1024;
    public final static int OUT_BUFFER_SIZE = 1024;

    @Getter
    private Input input;
    @Getter
    private Output output;

    private InputChunked inputChunked;
    private OutputChunked outputChunked;

    /**
     * Underlying transport
     */
    private final TSocket transport_;
    private final int iBufferSize;
    private final int oBufferSize;
    private final boolean chunkedFlag;

    public static class Factory extends TTransportFactory {
        private final int iBufferSize;
        private final int oBufferSize;
        private final boolean chunkedFlag;

        public Factory() {
            this(IN_BUFFER_SIZE, OUT_BUFFER_SIZE, true);
        }

        public Factory(int iBufferSize, int oBufferSize, boolean chunkedFlag) {
            this.iBufferSize = iBufferSize;
            this.oBufferSize = oBufferSize;
            this.chunkedFlag = chunkedFlag;
        }

        @Override
        public TTransport getTransport(TTransport base) {
            return new TKryoTransport((TSocket) base, iBufferSize, oBufferSize, chunkedFlag);
        }
    }

    /**
     * Constructor wraps around another transport
     */
    public TKryoTransport(TSocket transport) {
        this(transport, IN_BUFFER_SIZE, OUT_BUFFER_SIZE, true);
    }

    public TKryoTransport(TSocket transport, boolean chunkedFlag) {
        this(transport, IN_BUFFER_SIZE, OUT_BUFFER_SIZE, chunkedFlag);
    }

    public TKryoTransport(TSocket transport, int iBufferSize, int oBufferSize, boolean chunkedFlag) {
        transport_ = transport;
        this.iBufferSize = iBufferSize;
        this.oBufferSize = oBufferSize;
        this.chunkedFlag = chunkedFlag;
        if (isOpen()) {
            initStream();
        }
    }

    private void initStream() {
        input = new Input(transport_.inputStream_, iBufferSize);
        output = new Output(transport_.outputStream_, oBufferSize);
        if (chunkedFlag) {
            inputChunked = new InputChunked(input);
            outputChunked = new OutputChunked(output);
            input = inputChunked;
            output = outputChunked;
        }

        transport_.inputStream_ = input;
        transport_.outputStream_ = output;
    }

    public void open() throws TTransportException {
        if (!isOpen()) {
            transport_.open();
            initStream();
        }
    }

    public boolean isOpen() {
        return transport_.isOpen();
    }

    public void close() {
        transport_.close();
    }

    public int read(byte[] buf, int off, int len) { //throws TTransportException
        int got = input.read(buf, off, len);
        if (got > 0) {
            return got;
        }

        if (chunkedFlag) {
            // Read another frame of data
            inputChunked.nextChunk();
        }

        return input.read(buf, off, len);
    }

    @Override
    public byte[] getBuffer() {
        return input.getBuffer();
    }

    @Override
    public int getBufferPosition() {
        return input.position();
    }

    @Override
    public int getBytesRemainingInBuffer() {
        return input.limit() - input.position();
    }

    @Override
    public void consumeBuffer(int len) {
        input.setPosition(getBufferPosition() + len);
    }

    public void write(byte[] buf, int off, int len) throws TTransportException {
        output.write(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException {
        byte[] buf = output.toBytes();
        int len = output.position(); //int len = buf.length;
        output.reset();

        output.write(buf, 0, len); //transport_.write(buf, 0, len);
        if (chunkedFlag) {
            outputChunked.endChunk();
        }
        output.flush(); //transport_.flush();
    }

}
