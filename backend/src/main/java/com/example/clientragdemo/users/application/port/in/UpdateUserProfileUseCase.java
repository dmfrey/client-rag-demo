package com.example.clientragdemo.users.application.port.in;

import com.example.clientragdemo.users.application.domain.model.User;

public interface UpdateUserProfileUseCase {

    User execute(UpdateUserProfileCommand command);

    record UpdateUserProfileCommand(String username, String firstName, String lastName, String email) {}
}
