package com.example.file_storage;

import com.example.file_storage.config.TestStorageConfig;
import com.example.file_storage.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import({TestcontainersConfiguration.class, TestStorageConfig.class})
@SpringBootTest
@ActiveProfiles("test")
class FileStorageApplicationTests {

	@Test
	void contextLoads() {
	}

}
