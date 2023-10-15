package com.halloffame.thriftjsoa.core.server;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务模式-netty
 * @author zhuwx
 */
@Slf4j
public class TNettyServer extends TServer {

    public static final int EXECUTOR_SERVICE_KEEP_ALIVE_TIME = 60;

    private ServerBootstrap b;

    private ChannelFuture f;

    private int port;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ExecutorService executorService_;

    @SneakyThrows
    public TNettyServer(TNettyServer.Args args) {
        super(args);

        // Configure SSL.
        final SslContext sslCtx;
        if (args.ssl) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(args.nThreads);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
            .childOption(ChannelOption.SO_TIMEOUT, args.soTimeout)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    p.addLast(new ServerHandler());
                }
            });

        this.port = args.port;
        this.executorService_ = args.executorService != null ?
                args.executorService : createDefaultExecutorService(args);
    }

    private static ExecutorService createDefaultExecutorService(TNettyServer.Args args) {
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
            // Bind and start to accept incoming connections.
            f = b.bind(port).sync();
            this.setServing(true);

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();

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

            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();

            this.setServing(false);
        }
    }

    @Override
    public void stop() {
        this.stopped_ = true;

        if (f != null) {
            Channel c = f.channel();
            //c.flush();
            c.close();
        }
    }

    @Override
    public void setShouldStop(boolean shouldStop) {
        this.stopped_ = shouldStop;
        if (shouldStop) {
            stop();
        }
    }

    /**
     * Handles a server-side channel.
     */
    public class ServerHandler extends SimpleChannelInboundHandler<Object> {

        private PipedInputStream reqPis = new PipedInputStream();
        private PipedOutputStream reqPos;
        //private PipedInputStream respPis;
        //private PipedOutputStream respPos = new PipedOutputStream();

        private ByteArrayOutputStream respBaos = new ByteArrayOutputStream();

        @SneakyThrows
        public ServerHandler() {
            reqPos = new PipedOutputStream(reqPis);
            //respPis = new PipedInputStream(respPos);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            /* executorService_.execute(() -> {
                byte[] buf = new byte[1024];
                try {
                    for(int bytesRead = respPis.read(buf); bytesRead != -1; bytesRead = respPis.read(buf)) {
                        //ctx.writeAndFlush(Unpooled.copiedBuffer(buf, 0, bytesRead));
                        ctx.write(Unpooled.copiedBuffer(buf, 0, bytesRead));
                    }
                } catch (IOException e) {
                    log.error("", e);
                } finally {
                    try {
                        respPis.close();
                    } catch (IOException e) {
                        log.error("", e);
                    }
                    ctx.flush();
                    ctx.close();
                }
            }); */

            executorService_.execute(() -> {
                TTransport client = null;
                TProcessor processor = null;
                TTransport inputTransport = null;
                TTransport outputTransport = null;
                TProtocol inputProtocol = null;
                TProtocol outputProtocol = null;
                ServerContext connectionContext = null;
                try {
                    //client = serverTransport_.accept();
                    //client = new TIOStreamTransport(reqPis, respPos);
                    client = new TIOStreamTransport(reqPis, respBaos);

                    if (client != null) {
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
                            ctx.writeAndFlush(Unpooled.copiedBuffer(respBaos.toByteArray()));
                            respBaos.reset();
                            if(!processRet) {
                                break;
                            }
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
                ctx.flush();
                ctx.close();
            });
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            //System.out.println(new String(bytes));
            reqPos.write(bytes);
            //ctx.writeAndFlush(Unpooled.copiedBuffer("yyy", CharsetUtil.UTF_8)); //.addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            //cause.printStackTrace();
            log.error("", cause);
            ctx.close();
        }
    }

    public static class Args extends AbstractServerArgs<TNettyServer.Args> {
        /**
         * workerEventLoopGroup线程数
         */
        private int nThreads;

        /**
         * 读超时时间
         */
        private int soTimeout;

        /**
         * 监听端口
         */
        private int port = CommonServer.PORT;

        /**
         * 是否加密传输
         */
        private boolean ssl;

        /**
         * ExecutorService
         */
        private ExecutorService executorService;

        public Args(TServerTransport transport) {
            super(transport);
        }

        public TNettyServer.Args nThreads(int nThreads) {
            this.nThreads = nThreads;
            return this;
        }

        public TNettyServer.Args soTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
            return this;
        }

        public TNettyServer.Args port(int port) {
            this.port = port;
            return this;
        }

        public TNettyServer.Args ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public TNettyServer.Args executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }
    }
}
