package org.apache.thrift.transport;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TSocketAdapter传输
 * @author zhuwx
 */
public class TSocketAdapterTransport extends TTransport {

    /**
     * Underlying transport
     */
    private final TSocket transport_;

    public static class Factory extends TTransportFactory {
        @Override
        public TTransport getTransport(TTransport base) {
            return new TSocketAdapterTransport((TSocket) base);
        }
    }

    public TSocketAdapterTransport(TSocket transport) {
        transport_ = transport;
    }

    public InputStream getInputStream() {
        return transport_.inputStream_;
    }
    public OutputStream getOutputStream() {
        return transport_.outputStream_;
    }

    public void open() throws TTransportException {
        if (!isOpen()) {
            transport_.open();
        }
    }

    public boolean isOpen() {
        return transport_.isOpen();
    }

    public void close() {
        transport_.close();
    }

    public int read(byte[] buf, int off, int len) throws TTransportException {
        return transport_.read(buf, off, len);
    }

    public void write(byte[] buf, int off, int len) throws TTransportException {
        transport_.write(buf, off, len);
    }

}
