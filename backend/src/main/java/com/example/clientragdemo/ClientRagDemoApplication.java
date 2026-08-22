package com.example.clientragdemo;

import com.example.clientragdemo.configuration.LiquibaseRuntimeHints;
import com.example.clientragdemo.configuration.OpenAiRuntimeHints;
import com.example.clientragdemo.configuration.PdfBoxRuntimeHints;
import com.example.clientragdemo.configuration.PoiRuntimeHints;
import com.example.clientragdemo.configuration.XmlBeansRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportRuntimeHints({ LiquibaseRuntimeHints.class, OpenAiRuntimeHints.class, PdfBoxRuntimeHints.class, PoiRuntimeHints.class, XmlBeansRuntimeHints.class })
public class ClientRagDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientRagDemoApplication.class, args);
	}

}
