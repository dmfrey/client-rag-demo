package com.example.clientragdemo.users.application.domain.service;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.in.RegisterUserUseCase;
import com.example.clientragdemo.users.application.port.out.LoadUserByUsernamePort;
import com.example.clientragdemo.users.application.port.out.SaveUserPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserByUsernamePort loadUserByUsernamePort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoder passwordEncoder;

    RegisterUserService(LoadUserByUsernamePort loadUserByUsernamePort, SaveUserPort saveUserPort, PasswordEncoder passwordEncoder) {
        this.loadUserByUsernamePort = loadUserByUsernamePort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User execute(RegisterUserCommand command) {
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (command.password() == null || command.password().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        if (loadUserByUsernamePort.loadByUsername(command.username()).isPresent()) {
            throw new UsernameAlreadyTakenException(command.username());
        }

        User user = new User(null, command.username(), passwordEncoder.encode(command.password()), Instant.now());

        return saveUserPort.save(user);
    }
}
