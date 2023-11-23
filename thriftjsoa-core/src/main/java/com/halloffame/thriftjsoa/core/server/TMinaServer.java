package com.halloffame.thriftjsoa.core.server;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpServer;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

@Slf4j
public class TMinaServer extends TServer {

    public static final int EXECUTOR_SERVICE_KEEP_ALIVE_TIME = 60;

    private final int port;

    private final NioTcpServer acceptor;

    private final ExecutorService executorService_;

    private static final CountDownLatch countDownLatch = new CountDownLatch(1);

    @SneakyThrows
    public TMinaServer(TMinaServer.Args args) {
        super(args);
        acceptor = new NioTcpServer();
        // create the filter chain for this service
        //acceptor.setFilters(new LoggingFilter("LoggingFilter1"));
        acceptor.setIoHandler(new ServerHandler());

        this.port = args.port;
        this.executorService_ = args.executorService != null ?
                args.executorService : createDefaultExecutorService(args);
    }

    private static ExecutorService createDefaultExecutorService(TMinaServer.Args args) {
        SynchronousQueue<Runnable> executorQueue = new SynchronousQueue<>();
        return new ThreadPoolExecutor(5, //Runtime.getRuntime().availableProcessors() * 2 + 1
                2000, //Integer.MAX_VALUE
                EXECUTOR_SERVICE_KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                executorQueue);
    }

    /**
     * The run method fires up the server and gets things going.
     */
    @SneakyThrows
    @Override
    public void serve() {
        if (!this.stopped_) {
            if (this.eventHandler_ != null) {
                this.eventHandler_.preServe();
            }

            final SocketAddress address = new InetSocketAddress(port);
            acceptor.bind(address);
            this.setServing(true);

            countDownLatch.await();

            executorService_.shutdown();
            // Loop until awaitTermination finally does return without a interrupted
            // exception. If we don't do this, then we'll shut down prematurely. We want
            // to let the executorService clear it's task queue, closing client sockets
            // appropriately.
            long timeoutMS = TimeUnit.SECONDS.toMillis(EXECUTOR_SERVICE_KEEP_ALIVE_TIME);
            long now = System.currentTimeMillis();
            boolean isTermination = false;
            while (timeoutMS >= 0) {
                try {
                    isTermination = executorService_.awaitTermination(timeoutMS, TimeUnit.MILLISECONDS);
                    break;
                } catch (InterruptedException ix) {
                    long newnow = System.currentTimeMillis();
                    timeoutMS -= (newnow - now);
                    now = newnow;
                }
            }
            if (!isTermination) {
                executorService_.shutdownNow();
            }

            this.setServing(false);
        }
    }

    @Override
    public void stop() {
        this.stopped_ = true;

        if (acceptor != null) {
            acceptor.unbind();
            countDownLatch.countDown();
        }
    }

    @Override
    public void setShouldStop(boolean shouldStop) {
        this.stopped_ = shouldStop;
        if (shouldStop) {
            stop();
        }
    }

    public class ServerHandler extends AbstractIoHandler {

        private final PipedInputStream reqPis = new PipedInputStream();
        private final PipedOutputStream reqPos;

        private final ByteArrayOutputStream respBaos = new ByteArrayOutputStream();

        @SneakyThrows
        public ServerHandler() {
            reqPos = new PipedOutputStream(reqPis);
        }

        @Override
        public void sessionOpened(final IoSession session) {

            executorService_.execute(() -> {
                TTransport client = null;
                TProcessor processor = null;
                TTransport inputTransport = null;
                TTransport outputTransport = null;
                TProtocol inputProtocol = null;
                TProtocol outputProtocol = null;
                ServerContext connectionContext = null;
                try {
                    client = new TIOStreamTransport(reqPis, respBaos);

                    processor = processorFactory_.getProcessor(client);
                    inputTransport = inputTransportFactory_.getTransport(client);
                    outputTransport = outputTransportFactory_.getTransport(client);
                    inputProtocol = inputProtocolFactory_.getProtocol(inputTransport);
                    outputProtocol = outputProtocolFactory_.getProtocol(outputTransport);
                    if (eventHandler_ != null) {
                        connectionContext = eventHandler_.createContext(inputProtocol, outputProtocol);
                    }
                    while (true) {
                        if (eventHandler_ != null) {
                            eventHandler_.processContext(connectionContext, inputTransport, outputTransport);
                        }
                        boolean processRet = processor.process(inputProtocol, outputProtocol);

                        session.write(respBaos.toByteArray());

                        //final String welcomeStr = "welcome\n";
                        //final ByteBuffer bf = ByteBuffer.allocate(welcomeStr.length());
                        //bf.put(welcomeStr.getBytes());
                        //bf.flip();
                        //session.write(bf);

                        respBaos.reset();
                        if(!processRet) {
                            break;
                        }
                    }
                } catch (TTransportException ttx) {
                    // Client died, just move on
                } catch (TException tx) {
                    if (!stopped_) {
                        log.error("Thrift error occurred during processing of message.", tx);
                    }
                } catch (Exception x) {
                    if (!stopped_) {
                        log.error("Error occurred during processing of message.", x);
                    }
                }

                if (eventHandler_ != null) {
                    eventHandler_.deleteContext(connectionContext, inputProtocol, outputProtocol);
                }

                if (inputTransport != null) {
                    inputTransport.close();
                }
                if (outputTransport != null) {
                    outputTransport.close();
                }
                session.close(true);
            });
        }

        @SneakyThrows
        @Override
        public void messageReceived(IoSession session, Object message) {
            if (message instanceof ByteBuffer) {
                ByteBuffer buf = (ByteBuffer) message;
                reqPos.write(buf.array());
            }
        }
    }

    public static class Args extends AbstractServerArgs<TMinaServer.Args> {
        /**
         * 监听端口
         */
        private int port = CommonServer.PORT;

        /**
         * ExecutorService
         */
        private ExecutorService executorService;

        public Args(TServerTransport transport) {
            super(transport);
        }

        public TMinaServer.Args port(int port) {
            this.port = port;
            return this;
        }

        public TMinaServer.Args executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }
    }
}
