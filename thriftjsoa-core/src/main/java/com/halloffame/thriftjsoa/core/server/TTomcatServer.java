package com.halloffame.thriftjsoa.core.server;

import com.halloffame.thriftjsoa.core.common.CommonServer;
import lombok.SneakyThrows;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TExtensibleServlet;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Http-TomcatServer
 * @author zhuwx
 */
public class TTomcatServer extends TServer {

    /**
     * 嵌入式tomcat的basedir默认值
     */
    public static final String TOMCAT_BASEDIR = "./tjtomcat/";

    private Tomcat tomcat;

    public TTomcatServer(TTomcatServer.Args args) {
        super(args);

        File file = new File(args.baseDir + "webapps/");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }

        tomcat = new Tomcat();
        tomcat.setPort(args.port);
        tomcat.setBaseDir(args.baseDir);
        Context context = tomcat.addContext("", "/");
        tomcat.addServlet("", "tServlet", new TServlet(this.inputProtocolFactory_,
                this.outputProtocolFactory_, this.processorFactory_.getProcessor(null), eventHandler_));

        String path = args.httpPath;
        if (path == null || "".equals(path.trim())) {
            path = "*";
        }
        context.addServletMappingDecoded("/" + path, "tServlet");

        ProtocolHandler handler = tomcat.getConnector().getProtocolHandler();
        if (handler instanceof AbstractProtocol) {
            AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
            protocol.setAcceptCount(args.acceptCount);
            protocol.setMaxConnections(args.maxConnections);
            protocol.setMaxThreads(args.maxThreads);
            protocol.setMinSpareThreads(args.minSpareThreads);
            protocol.setConnectionTimeout(args.connectionTimeout);
        }
    }

    @SneakyThrows
    @Override
    public void serve() {
        if (!this.stopped_) {
            if (this.eventHandler_ != null) {
                this.eventHandler_.preServe();
            }
            tomcat.start();
            this.setServing(true);

            tomcat.getServer().await();

            tomcat.destroy();
            this.setServing(false);
        }
    }

    @SneakyThrows
    @Override
    public void stop() {
        this.stopped_ = true;
        if (Objects.nonNull(tomcat)) {
            tomcat.stop();
        }
    }

    @Override
    public void setShouldStop(boolean shouldStop) {
        this.stopped_ = shouldStop;
        if (shouldStop) {
            stop();
        }
    }

    static class TServlet extends TExtensibleServlet {

        private TProtocolFactory inProtocolFactory;
        private TProtocolFactory outProtocolFactory;
        private TProcessor processor;
        protected TServerEventHandler eventHandler_;

        private Collection<Map.Entry<String, String>> customHeaders;

        public TServlet(TProtocolFactory inProtocolFactory, TProtocolFactory outProtocolFactory, TProcessor processor,
                        TServerEventHandler eventHandler_) {
            this.inProtocolFactory = inProtocolFactory;
            this.outProtocolFactory = outProtocolFactory;
            this.processor = processor;
            this.eventHandler_ = eventHandler_;
            this.customHeaders = new ArrayList<>();
        }

        @Override
        protected TProtocolFactory getInProtocolFactory() {
            return inProtocolFactory;
        }

        @Override
        protected TProtocolFactory getOutProtocolFactory() {
            return outProtocolFactory;
        }

        @Override
        protected TProcessor getProcessor() {
            return processor;
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            TTransport inTransport = null;
            TTransport outTransport = null;

            try {
                response.setContentType("application/x-thrift");

                if (null != this.customHeaders) {
                    for (Map.Entry<String, String> header : this.customHeaders) {
                        response.addHeader(header.getKey(), header.getValue());
                    }
                }

                InputStream in = request.getInputStream();
                OutputStream out = response.getOutputStream();

                TTransport transport = new TIOStreamTransport(in, out);
                inTransport = transport;
                outTransport = transport;

                TProtocol inProtocol = inProtocolFactory.getProtocol(inTransport);
                TProtocol outProtocol = outProtocolFactory.getProtocol(outTransport);

                ServerContext connectionContext = null;
                if (eventHandler_ != null) {
                    connectionContext = eventHandler_.createContext(inProtocol, outProtocol);
                    eventHandler_.processContext(connectionContext, inTransport, outTransport);
                }

                processor.process(inProtocol, outProtocol);
                out.flush();

                if (eventHandler_ != null) {
                    eventHandler_.deleteContext(connectionContext, inProtocol, outProtocol);
                }
            } catch (TException te) {
                throw new ServletException(te);
            }
        }
    }

    public static class Args extends AbstractServerArgs<TTomcatServer.Args> {
        /**
         * 嵌入式tomcat的basedir
         */
        private String baseDir = TOMCAT_BASEDIR;

        /**
         * 嵌入式tomcat的监听端口
         */
        private int port = CommonServer.PORT;

        /**
         * Maximum number of connections that the server accepts and processes at any
         * given time. Once the limit has been reached, the operating system may still
         * accept connections based on the "acceptCount" property.
         */
        private int maxConnections = 8192;

        /**
         * Maximum queue length for incoming connection requests when all possible request
         * processing threads are in use.
         */
        private int acceptCount = 100;

        /**
         * Maximum amount of worker threads.
         */
        private int maxThreads = 200;

        /**
         * Minimum amount of worker threads.
         */
        private int minSpareThreads = 10;

        /**
         * Amount of time the connector will wait, after accepting a connection, for the
         * request URI line to be presented.
         */
        //private Duration connectionTimeout;
        private int connectionTimeout;

        /**
         * 请求path路径
         */
        private String httpPath = "";

        public Args(TServerTransport transport) {
            super(transport);
        }

        public TTomcatServer.Args baseDir(String baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        public TTomcatServer.Args port(int port) {
            this.port = port;
            return this;
        }

        public TTomcatServer.Args maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public TTomcatServer.Args acceptCount(int acceptCount) {
            this.acceptCount = acceptCount;
            return this;
        }

        public TTomcatServer.Args maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public TTomcatServer.Args minSpareThreads(int minSpareThreads) {
            this.minSpareThreads = minSpareThreads;
            return this;
        }

        public TTomcatServer.Args connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public TTomcatServer.Args httpPath(String httpPath) {
            this.httpPath = httpPath;
            return this;
        }
    }
}
