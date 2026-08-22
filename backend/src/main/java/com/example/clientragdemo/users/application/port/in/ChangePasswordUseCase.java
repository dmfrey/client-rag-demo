package com.example.clientragdemo.users.application.port.in;

public interface ChangePasswordUseCase {

    void execute(ChangePasswordCommand command);

    record ChangePasswordCommand(String username, String currentPassword, String newPassword) {}
}
