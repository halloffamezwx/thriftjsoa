package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.aop.ClientAnnotation;
import com.halloffame.thriftjsoa.common.CommonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.stereotype.Component;
import thrift.test.ThriftTest;
import thrift.test.User;

/**
 * 客户端（测试）
 */
@Component
public class TestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class.getName());

    public static void main(String [] args) throws Exception {
        AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");
        TestClient testClient = context.getBean(TestClient.class);
        testClient.test();
    }

    /**
     * test方法会被ClientAspect拦截，然后根据配置创建ThriftTest.Client对象保存在CommonClient的ThreadLocal变量里
     * CommonClient.getClient将从ThreadLocal变量里取得ThriftTest.Client对象
     * ThriftTest.Client对象的资源释放将在ClientAspect的finally块里进行
     */
    @ClientAnnotation(clientClassArr = {ThriftTest.Client.class})
    public void test() throws Exception {
        ThriftTest.Client thriftTestClient = CommonClient.getClient(ThriftTest.Client.class);
        User user = thriftTestClient.getUser(2); //getUser就是ThriftTest.thrift所定义的接口
        LOGGER.info("名字：{}", user.getName());
    }
}
