package com.cenziang.itsmserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = "itsm.auth.seed.enabled=false")
class ItsmServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
