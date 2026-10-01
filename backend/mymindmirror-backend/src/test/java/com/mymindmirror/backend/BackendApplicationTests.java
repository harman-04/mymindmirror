package com.mymindmirror.backend;

import com.mymindmirror.backend.config.TestVectorStoreConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestVectorStoreConfig.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
