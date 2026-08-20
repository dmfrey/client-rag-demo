package com.example.clientragdemo.users.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@ComponentScan(basePackages = "com.example.clientragdemo.users")
@EnableJdbcRepositories(basePackages = "com.example.clientragdemo.users.adapter.out.persistence")
class UsersConfiguration {
}
