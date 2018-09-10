package com.halloffame.thriftjsoa;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import thrift.test.ThriftTest;
import thrift.test.User;

/**
 * 具体的业务逻辑类
 * 实现ThriftTest.thrift里的getUser接口
 */
@Component //由spring容器实例化管理等
public class TestHandler implements ThriftTest.Iface {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
	
	@Override
	public User getUser(int id) throws TException {
		LOGGER.info("id==>{}", id); 
		if (id == 2 ) {
			User user = new User();
			user.setId(2);
			user.setName("另外一个烟火");
			return user;
		}
		return null;
	}
}
