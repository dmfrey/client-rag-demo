package com.example.clientragdemo.users.application.domain.service;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.in.GetUserUseCase;
import com.example.clientragdemo.users.application.port.out.LoadUserByUsernamePort;
import org.springframework.stereotype.Service;

@Service
class GetUserService implements GetUserUseCase {

    private final LoadUserByUsernamePort loadUserByUsernamePort;

    GetUserService(LoadUserByUsernamePort loadUserByUsernamePort) {
        this.loadUserByUsernamePort = loadUserByUsernamePort;
    }

    @Override
    public User execute(GetUserQuery query) {
        return loadUserByUsernamePort.loadByUsername(query.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + query.username()));
    }
}
