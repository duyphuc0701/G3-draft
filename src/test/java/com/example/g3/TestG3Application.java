package com.example.g3;

import org.springframework.boot.SpringApplication;

public class TestG3Application {

	public static void main(String[] args) {
		SpringApplication.from(G3Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
