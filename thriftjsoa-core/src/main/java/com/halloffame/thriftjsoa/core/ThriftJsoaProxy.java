package com.halloffame.thriftjsoa.core;

import com.halloffame.thriftjsoa.core.base.ConnectionFactory;
import com.halloffame.thriftjsoa.core.base.TProtocolWrap;
import com.halloffame.thriftjsoa.core.base.TjProtocol;
import com.halloffame.thriftjsoa.core.common.CommonClient;
import com.halloffame.thriftjsoa.core.common.CommonServer;
import com.halloffame.thriftjsoa.core.common.CreateLoadBalanceResult;
import com.halloffame.thriftjsoa.core.config.BaseProxyConfig;
import com.halloffame.thriftjsoa.core.config.common.ProcessorConfig;
import com.halloffame.thriftjsoa.core.constant.LoadBalanceType;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceAbstract;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

import java.util.ArrayList;
import java.util.List;

/**
 * ThriftJsoa代理
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaProxy {

    /**
     * 代理端配置
     */
    @Getter
    private BaseProxyConfig proxyConfig;

    public ThriftJsoaProxy() {
        proxyConfig = new BaseProxyConfig();
    }
    public ThriftJsoaProxy(BaseProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
    public ThriftJsoaProxy(int port, String zkConnStr) {
        proxyConfig = new BaseProxyConfig();
        proxyConfig.getServerConfig().setPort(port);
        proxyConfig.getLoadBalanceClientConfig().setLoadBalanceType(LoadBalanceType.RANDOM_WEIGHT.getCode());
        proxyConfig.getLoadBalanceClientConfig().getZkRegisterConfig().setZkConnStr(zkConnStr);
        proxyConfig.getLoadBalanceClientConfig().setInTjServer(true);
    }

    /**
     * 启动运行
     */
    public void run() throws Exception {
        CreateLoadBalanceResult createLoadBalanceResult = CommonClient.createLoadBalance(proxyConfig.getLoadBalanceClientConfig());

        List<ProcessorConfig> processorConfigs = new ArrayList<>();
        processorConfigs.add(new ProcessorConfig().setTProcessor(new ProxyProcessor(createLoadBalanceResult.getLoadBalance()))); //自定义的一个processor，非thrift生成的代码
        proxyConfig.getServerConfig().setProcessorConfigs(processorConfigs);

        log.info("starting the proxy on port {}...", proxyConfig.getServerConfig().getPort());
        CommonServer.serve(proxyConfig.getServerConfig());
    }

    /**
     * 代理端自定义Processor
     * 主要功能是读取客户端发来的信息，然后通过负载均衡选择对应连接工厂的连接来转发到服务端
     */
    public class ProxyProcessor implements TProcessor {

        private LoadBalanceAbstract loadBalance;

        public ProxyProcessor(LoadBalanceAbstract loadBalance) {
            this.loadBalance = loadBalance;
        }

        @Override
        public boolean process(TProtocol in, TProtocol out) throws TException {
            ConnectionFactory connectionFactory = null;
            TTransport tTransport = null;
            TjProtocol inTj = (TjProtocol) in;
            TjProtocol outTj = (TjProtocol) out;

            try {
                TMessage clientMsg = inTj.readMessageBegin();

                log.debug("ProxyProcessor 收到请求：{}", clientMsg);
                //log.debug("ProxyProcessor traceId={}", ThriftJsoaUtil.getTraceId());
                //log.debug("ProxyProcessor appId={}", ThriftJsoaUtil.getAppId());

                //通过负载均衡取得TProtocol来发消息到对应服务端
                LoadBalanceBean loadBalanceBean = loadBalance.getLoadBalanceBean();
                connectionFactory = loadBalanceBean.getConnectionFactory();

                TProtocolWrap tProtocolWrap = loadBalanceBean.getProtocolWrap();
                tTransport = tProtocolWrap.getTTransport();
                TjProtocol inTjProtocol = tProtocolWrap.getInTjProtocol();
                TjProtocol outTjProtocol = tProtocolWrap.getOutTjProtocol();

                outTjProtocol.writeMessageBegin(clientMsg);

                inTj.readWriteObj(outTjProtocol);

                inTj.readMessageEnd();
                outTjProtocol.writeMessageEnd();
                outTjProtocol.getTransport().flush();

                TMessage serverMsg = inTjProtocol.readMessageBegin();
                outTj.writeMessageBegin(serverMsg);

                if (serverMsg.type == TMessageType.EXCEPTION) {
                    TApplicationException x = TApplicationException.read(inTjProtocol);
                    x.write(outTj);
                    inTjProtocol.readMessageEnd();
                    outTj.writeMessageEnd();
                    //throw x;
                    return true;
                }
                //if (serverMsg.seqid != clientMsg.seqid) {
                //    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, clientMsg.name + " failed: out of sequence response");
                //}

                inTjProtocol.readWriteObj(outTj);

                inTjProtocol.readMessageEnd();
                outTj.writeMessageEnd();
                outTj.getTransport().flush();

                return true;
            } finally {
                if (connectionFactory != null && tTransport != null) {
                    connectionFactory.releaseConnection(tTransport); //归还tTransport到连接工厂
                }
            }
        }

    }

}
