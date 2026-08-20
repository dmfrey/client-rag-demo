package com.example.clientragdemo.chat.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@ComponentScan(basePackages = "com.example.clientragdemo.chat")
@EnableJdbcRepositories(basePackages = "com.example.clientragdemo.chat.adapter.out.persistence")
class ChatConfiguration {
}
