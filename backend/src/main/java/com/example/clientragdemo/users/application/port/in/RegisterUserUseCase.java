package com.example.clientragdemo.users.application.port.in;

import com.example.clientragdemo.users.application.domain.model.User;

public interface RegisterUserUseCase {

    User execute(RegisterUserCommand command);

    record RegisterUserCommand(String username, String password) {}
}
