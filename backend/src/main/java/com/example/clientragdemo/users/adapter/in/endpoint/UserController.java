package com.example.clientragdemo.users.adapter.in.endpoint;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.in.ChangePasswordUseCase;
import com.example.clientragdemo.users.application.port.in.ChangePasswordUseCase.ChangePasswordCommand;
import com.example.clientragdemo.users.application.port.in.UpdateUserProfileUseCase;
import com.example.clientragdemo.users.application.port.in.UpdateUserProfileUseCase.UpdateUserProfileCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    UserController(UpdateUserProfileUseCase updateUserProfileUseCase, ChangePasswordUseCase changePasswordUseCase) {
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PatchMapping("/me")
    UserResponse updateProfile(@RequestBody UpdateProfileRequest request, Authentication authentication) {
        User user = updateUserProfileUseCase.execute(new UpdateUserProfileCommand(
                authentication.getName(), request.firstName(), request.lastName(), request.email()));
        return toResponse(user);
    }

    @PutMapping("/me/password")
    ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        changePasswordUseCase.execute(new ChangePasswordCommand(authentication.getName(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.username(), user.firstName(), user.lastName(), user.email());
    }

    record UpdateProfileRequest(String firstName, String lastName, String email) {}

    record ChangePasswordRequest(String currentPassword, String newPassword) {}

    record UserResponse(Long id, String username, String firstName, String lastName, String email) {}
}
