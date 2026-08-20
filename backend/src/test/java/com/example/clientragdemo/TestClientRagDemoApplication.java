package com.example.clientragdemo;

import org.springframework.boot.SpringApplication;

public class TestClientRagDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClientRagDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
