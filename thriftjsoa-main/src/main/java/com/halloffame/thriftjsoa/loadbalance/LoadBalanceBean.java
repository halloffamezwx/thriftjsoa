package com.halloffame.thriftjsoa.loadbalance;

import com.halloffame.thriftjsoa.base.ConnectionFactory;
import com.halloffame.thriftjsoa.base.TProtocolWrap;
import lombok.Data;

/**
 * 负载均衡的返回结果
 * @author zhuwx
 */
@Data
public class LoadBalanceBean {

    /**
     * 选择的连接工厂
     */
	private ConnectionFactory connectionFactory;

    /**
     * 选择的连接工厂里的TProtocol
     */
    private TProtocolWrap protocolWrap;

}
