package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.base.ConnectionFactory;
import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.common.CreateLoadBalanceResult;
import com.halloffame.thriftjsoa.config.BaseProxyConfig;
import com.halloffame.thriftjsoa.constant.LoadBalanceType;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;

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
        proxyConfig.getLoadBalanceClientConfig().getZkConnConfig().setZkConnStr(zkConnStr);
        proxyConfig.getLoadBalanceClientConfig().setInTjServer(true);
    }

    /**
     * 启动运行
     */
    public void run() throws Exception {
        CreateLoadBalanceResult createLoadBalanceResult = CommonClient.createLoadBalance(proxyConfig.getLoadBalanceClientConfig());

        TProcessor processor = proxyConfig.getServerConfig().getProcessor();
        if (processor == null) {
            processor = new ProxyProcessor(createLoadBalanceResult.getLoadBalance()); //自定义的一个processor，非thrift生成的代码
            proxyConfig.getServerConfig().setProcessor(processor);
        }

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
            TProtocol tProtocol = null; //通信协议

            try {
                TMessage clientMsg = in.readMessageBegin();

                log.debug("ProxyProcessor 收到请求：{}", clientMsg);
                log.debug("ProxyProcessor traceId={}", ThriftJsoaUtil.getTraceId());
                log.debug("ProxyProcessor appId={}", ThriftJsoaUtil.getAppId());

                //通过负载均衡取得TProtocol来发消息到对应服务端
                LoadBalanceBean loadBalanceBean = loadBalance.getLoadBalanceBean();
                connectionFactory = loadBalanceBean.getConnectionFactory();
                tProtocol = loadBalanceBean.getProtocolWrap().getTProtocol();

                tProtocol.writeMessageBegin(clientMsg);

                readWriteData(in, tProtocol);

                in.readMessageEnd();
                tProtocol.writeMessageEnd();
                tProtocol.getTransport().flush();

                TMessage serverMsg = tProtocol.readMessageBegin();
                out.writeMessageBegin(serverMsg);

                if (serverMsg.type == TMessageType.EXCEPTION) {
                    TApplicationException x = TApplicationException.read(tProtocol);
                    x.write(out);
                    tProtocol.readMessageEnd();
                    out.writeMessageEnd();
                    //throw x;
                    return true;
                }
                //if (serverMsg.seqid != clientMsg.seqid) {
                //    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, clientMsg.name + " failed: out of sequence response");
                //}

                readWriteData(tProtocol, out);

                tProtocol.readMessageEnd();
                out.writeMessageEnd();
                out.getTransport().flush();

                return true;
            } finally {
                if (connectionFactory != null && tProtocol != null) {
                    connectionFactory.releaseConnection(tProtocol); //归还tProtocol到连接工厂
                }
            }
        }

        /**
         * 读写数据
         */
        private void readWriteData(TProtocol in, TProtocol out) throws TException {

            TStruct tStruct = in.readStructBegin();
            out.writeStructBegin(tStruct);

            TField schemeField;
            while (true) {
                schemeField = in.readFieldBegin();

                if (schemeField.type == TType.STOP) {
                    break;
                } else {
                    out.writeFieldBegin(schemeField);
                }

                readWriteField(schemeField.type, in, out);

                in.readFieldEnd();
                out.writeFieldEnd();
            }
            out.writeFieldStop();

            in.readStructEnd();
            out.writeStructEnd();
        }

        /**
         * 读写字段数据
         */
        private void readWriteField(byte fieldtype, TProtocol in, TProtocol out) throws TException {

            switch (fieldtype) {
                case TType.VOID:
                    TProtocolUtil.skip(in, fieldtype);
                    break;
                case TType.BOOL:
                    out.writeBool(in.readBool());
                    break;
                case TType.BYTE:
                    out.writeByte(in.readByte());
                    break;
                case TType.DOUBLE:
                    out.writeDouble(in.readDouble());
                    break;
                case TType.I16:
                    out.writeI16(in.readI16());
                    break;
                case TType.I32:
                    out.writeI32(in.readI32());
                    break;
                case TType.I64:
                    out.writeI64(in.readI64());
                    break;
                case TType.STRING:
                    out.writeString(in.readString());
                    break;
                case TType.STRUCT:
                    readWriteData(in, out);
                    break;
                case TType.MAP:
                    /**
                     * readMapBegin返回的TMap对象有3个字段keyType，valueType，size，
                     * 就是map的key的类型，value的类型，map的大小，
                     * 从0到size循环按类型读取key和value就行了。
                     */
                    TMap tMap = in.readMapBegin();
                    out.writeMapBegin(tMap);
                    for (int i = 0; i < tMap.size; i++) {
                        readWriteField(tMap.keyType, in, out);
                        readWriteField(tMap.valueType, in, out);
                    }
                    in.readMapEnd();
                    out.writeMapEnd();
                    break;
                case TType.SET:
                    TSet tSet = in.readSetBegin();
                    out.writeSetBegin(tSet);
                    for (int i = 0; i < tSet.size; i++) {
                        readWriteField(tSet.elemType, in, out);
                    }
                    in.readSetEnd();
                    out.writeSetEnd();
                    break;
                case TType.LIST:
                    TList tList = in.readListBegin();
                    out.writeListBegin(tList);
                    for (int i = 0; i < tList.size; i++) {
                        readWriteField(tList.elemType, in, out);
                    }
                    in.readListEnd();
                    out.writeListEnd();
                    break;
                case TType.ENUM:
                    //Enum类型在序列化传输时是个i32
                    TProtocolUtil.skip(in, fieldtype);
                    break;
                default:
                    TProtocolUtil.skip(in, fieldtype);
            }
        }

    }

}
