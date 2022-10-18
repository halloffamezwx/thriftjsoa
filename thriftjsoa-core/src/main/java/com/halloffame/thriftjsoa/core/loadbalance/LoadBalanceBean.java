package com.halloffame.thriftjsoa.core.loadbalance;

import com.halloffame.thriftjsoa.core.base.ConnectionFactory;
import com.halloffame.thriftjsoa.core.base.TProtocolWrap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 负载均衡的返回结果
 * @author zhuwx
 */
//@Data
@Getter
@Setter
@ToString
public class LoadBalanceBean {

    /**
     * 选择的连接工厂
     */
	private ConnectionFactory connectionFactory;

    /**
     * 选择的连接工厂里的TProtocolWrap
     */
    private TProtocolWrap protocolWrap;

}
