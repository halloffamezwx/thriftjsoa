package com.halloffame.thriftjsoa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import thrift.test.ThriftTest;
import thrift.test.User;

public class TestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class.getName());
	public static final AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");
	
    public static void main(String [] args) throws Exception {
	    ThriftTest.Client testClient = (ThriftTest.Client)context.getBean("testClient");
	    
        //getUser就是ThriftTest.thrift所定义的接口
        User user = testClient.getUser(2); 
        LOGGER.info("名字：{}", user.getName());
        
        //context.registerShutdownHook();
        //context.close();
        testClient.getInputProtocol().getTransport().close();
    }
}
