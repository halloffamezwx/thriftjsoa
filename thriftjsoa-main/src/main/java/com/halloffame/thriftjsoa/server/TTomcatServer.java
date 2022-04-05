package com.halloffame.thriftjsoa.server;

import com.halloffame.thriftjsoa.common.CommonServer;
import lombok.SneakyThrows;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TExtensibleServlet;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TServerTransport;

import java.io.File;
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
                this.outputProtocolFactory_, this.processorFactory_.getProcessor(null)));
        context.addServletMappingDecoded("/*", "tServlet");
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
        }
        tomcat.getServer().await();

        this.setServing(false);
    }

    @SneakyThrows
    @Override
    public void setShouldStop(boolean shouldStop) {
        this.stopped_ = shouldStop;
        if (shouldStop && Objects.nonNull(tomcat)) {
            tomcat.stop();
        }
    }

    @SneakyThrows
    @Override
    public void stop() {
        this.stopped_ = true;
        tomcat.stop();
        tomcat.destroy();
    }

    static class TServlet extends TExtensibleServlet {

        private TProtocolFactory inProtocolFactory;
        private TProtocolFactory outProtocolFactory;
        private TProcessor processor;

        public TServlet(TProtocolFactory inProtocolFactory, TProtocolFactory outProtocolFactory, TProcessor processor) {
            this.inProtocolFactory = inProtocolFactory;
            this.outProtocolFactory = outProtocolFactory;
            this.processor = processor;
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
    }
}
