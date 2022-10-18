package com.halloffame.thriftjsoa.core.common;

import lombok.Data;
import org.apache.thrift.TServiceClient;

/**
 * 创建客户端返回结果
 * @author zhuwx
 */
@Data
public class CreateClientResult<T extends TServiceClient> {

    /**
     * 客户端对象
     */
    private T client;

}
