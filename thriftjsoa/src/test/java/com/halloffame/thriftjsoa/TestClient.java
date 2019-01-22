package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.annotation.ClientAnnotation;
import com.halloffame.thriftjsoa.common.CommonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import thrift.test.ThriftTest;
import thrift.test.User;

public class TestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class.getName());
	public static final AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");

	@ClientAnnotation(clientClassArr = {ThriftTest.Client.class})
    public static void main(String [] args) throws Exception {
        ThriftTest.Client testClient = CommonClient.getClient(ThriftTest.Client.class);
        User user = testClient.getUser(2); //getUser就是ThriftTest.thrift所定义的接口
        LOGGER.info("名字：{}", user.getName());
    }
}
