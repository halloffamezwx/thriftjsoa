package com.halloffame.thriftjsoa.sample.client;

import com.halloffame.thriftjsoa.sample.client.service.ClientTestService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    @Autowired
    private ClientTestService clientTestService;

    //@Test
    public void clientTest() throws Exception {
        clientTestService.clientTest();
    }

}
