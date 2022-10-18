package com.halloffame.thriftjsoa.core.base;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;

/**
 * TjProtocolUtil
 * @author zhuwx
 */
public class TjProtocolUtil extends TProtocolUtil {

    /**
     * Skips over the next data element from the provided input TProtocol object.
     *
     * @param prot  the protocol object to read from
     * @param type  the next value will be interpreted as this TType value.
     */
    public static void skip(TjProtocol prot, byte type)
            throws TException {
        if ( type == TType.STRUCT && prot.isDirectReadObj() ) {
            prot.readDirectObjects();
        } else {
            TProtocolUtil.skip(prot, type);
        }
    }

}
