package com.halloffame.thriftjsoa.boot.client.test;

import com.halloffame.thriftjsoa.boot.client.test.service.ClientTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    @Autowired
    private ClientTestService clientTestService;

    //@Test
    public void clientTest() throws Exception {
        clientTestService.clientTest();
    }

}
