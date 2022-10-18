package com.halloffame.thriftjsoa.core.config.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.thrift.TProcessor;

/**
 * 业务Processor配置
 * @author zhuwx
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ProcessorConfig {

    /**
     * 自定义名称，用于TMultiplexedProtocol的SERVICE_NAME
     */
    private String serviceName;

    /**
     * thrift业务处理的processor
     */
    private TProcessor tProcessor;
}
