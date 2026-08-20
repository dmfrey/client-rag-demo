package com.example.clientragdemo.users.application.port.out;

import com.example.clientragdemo.users.application.domain.model.User;

import java.util.Optional;

public interface LoadUserByUsernamePort {

    Optional<User> loadByUsername(String username);
}
