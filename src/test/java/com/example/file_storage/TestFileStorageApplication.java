package com.example.file_storage;

import org.springframework.boot.SpringApplication;

public class TestFileStorageApplication {

	public static void main(String[] args) {
		SpringApplication.from(FileStorageApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
