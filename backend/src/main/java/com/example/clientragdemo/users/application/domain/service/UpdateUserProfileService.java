package com.example.clientragdemo.users.application.domain.service;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.in.UpdateUserProfileUseCase;
import com.example.clientragdemo.users.application.port.out.LoadUserByUsernamePort;
import com.example.clientragdemo.users.application.port.out.SaveUserPort;
import org.springframework.stereotype.Service;

@Service
class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final LoadUserByUsernamePort loadUserByUsernamePort;
    private final SaveUserPort saveUserPort;

    UpdateUserProfileService(LoadUserByUsernamePort loadUserByUsernamePort, SaveUserPort saveUserPort) {
        this.loadUserByUsernamePort = loadUserByUsernamePort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public User execute(UpdateUserProfileCommand command) {
        User user = loadUserByUsernamePort.loadByUsername(command.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + command.username()));

        User updated = new User(user.id(), user.username(), user.passwordHash(),
                blankToNull(command.firstName()), blankToNull(command.lastName()), blankToNull(command.email()), user.createdAt());

        return saveUserPort.save(updated);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
