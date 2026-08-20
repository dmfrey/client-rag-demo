package com.example.clientragdemo.users.application.domain.service;

import com.example.clientragdemo.shared.exception.ConflictException;

public class UsernameAlreadyTakenException extends ConflictException {

    public UsernameAlreadyTakenException(String username) {
        super("Username already taken: " + username);
    }
}
