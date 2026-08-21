package com.example.clientragdemo;

import com.example.clientragdemo.configuration.LiquibaseRuntimeHints;
import com.example.clientragdemo.configuration.OpenAiRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportRuntimeHints({ LiquibaseRuntimeHints.class, OpenAiRuntimeHints.class })
public class ClientRagDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientRagDemoApplication.class, args);
	}

}
