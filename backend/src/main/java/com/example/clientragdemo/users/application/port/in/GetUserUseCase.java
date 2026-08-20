package com.example.clientragdemo.users.application.port.in;

import com.example.clientragdemo.users.application.domain.model.User;

public interface GetUserUseCase {

    User execute(GetUserQuery query);

    record GetUserQuery(String username) {}
}
