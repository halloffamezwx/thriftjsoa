package com.halloffame.thriftjsoa;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 代理（测试）
 */
public class TestProxy {
	public static void main(String[] args) {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-proxy.xml");
	}
}
