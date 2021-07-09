package com.halloffame.thriftjsoa.session;

import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ThriftJsoa会话Processor
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaSessionProcessor<T extends BaseService<?>> implements TProcessor {

    /**
     * 业务实现类
     */
    private T t;

    public ThriftJsoaSessionProcessor(T t) {
        this.t = t;
    }

    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
        TMessage tMessage = in.readMessageBegin();

        Class<?> clazz = t.getClass();
        List<Method> methods = ThriftJsoaUtil.getAllMethod(clazz);
        Method method = null;
        for (Method it : methods) {
            if (it.getName().equals(tMessage.name)) { //todo 需要判断方法重载的情况？
                method = it;
                break;
            }
        }

        if (method == null) {
            TProtocolUtil.skip(in, TType.STRUCT);
            in.readMessageEnd();
            TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '"+tMessage.name+"'");
            out.writeMessageBegin(new TMessage(tMessage.name, TMessageType.EXCEPTION, tMessage.seqid));
            x.write(out);
            out.writeMessageEnd();
            out.getTransport().flush();
            return true;
        }

        Object[] args;
        try {
            args = ThriftJsoaUtil.readData(in, method.getGenericParameterTypes());
        } catch (Exception e) {
            log.error("Protocol error processing " + tMessage.name, e);
            in.readMessageEnd();
            TApplicationException x = new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage());
            out.writeMessageBegin(new TMessage(tMessage.name, TMessageType.EXCEPTION, tMessage.seqid));
            x.write(out);
            out.writeMessageEnd();
            out.getTransport().flush();
            return true;
        }
        in.readMessageEnd();

        Object result;
        try {
            result = method.invoke(t, args);
        } catch (Exception e) {
            log.error("Internal error processing " + tMessage.name, e);
            TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                    "Internal error processing " + tMessage.name);
            out.writeMessageBegin(new TMessage(tMessage.name, TMessageType.EXCEPTION, tMessage.seqid));
            x.write(out);
            out.writeMessageEnd();
            out.getTransport().flush();
            return true;
        }

        Object[] retFields = {result};
        String[] retFieldNames = {"success"};
        Type[] retFieldTypes = {method.getGenericReturnType()};
        out.writeMessageBegin(new TMessage(tMessage.name, TMessageType.REPLY, tMessage.seqid));
        try {
            ThriftJsoaUtil.writeData(out, retFields, tMessage.name + "_result", retFieldNames, retFieldTypes);
        } catch (Exception e) {
            throw new TException(e);
        }
        out.writeMessageEnd();
        out.getTransport().flush();

        return true;
    }

}
