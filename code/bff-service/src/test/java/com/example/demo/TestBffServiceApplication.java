package com.example.demo;

import org.springframework.boot.SpringApplication;

public class TestBffServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(BffServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
