package com.example.clientragdemo.users.adapter.in.endpoint;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.in.GetUserUseCase;
import com.example.clientragdemo.users.application.port.in.GetUserUseCase.GetUserQuery;
import com.example.clientragdemo.users.application.port.in.RegisterUserUseCase;
import com.example.clientragdemo.users.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AuthController(RegisterUserUseCase registerUserUseCase,
                    GetUserUseCase getUserUseCase,
                    AuthenticationManager authenticationManager,
                    SecurityContextRepository securityContextRepository) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@RequestBody AuthRequest request) {
        User user = registerUserUseCase.execute(new RegisterUserCommand(request.username(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    ResponseEntity<UserResponse> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationResult);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = getUserUseCase.execute(new GetUserQuery(authenticationResult.getName()));
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/me")
    ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = getUserUseCase.execute(new GetUserQuery(authentication.getName()));
        return ResponseEntity.ok(toResponse(user));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.username());
    }

    record AuthRequest(String username, String password) {}

    record UserResponse(Long id, String username) {}
}
