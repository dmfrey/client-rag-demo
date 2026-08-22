package com.example.clientragdemo.users.adapter.in.endpoint;

import com.example.clientragdemo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
class UserControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void updateProfilePersistsAndReturnsUpdatedFields() {
        String username = "profile-" + UUID.randomUUID();
        HttpHeaders authHeaders = registerAndLogin(username);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/users/me", HttpMethod.PATCH,
                new HttpEntity<>(new UpdateProfileRequest("Bob", "Baker", "bob@example.com"), authHeaders),
                UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().firstName()).isEqualTo("Bob");
        assertThat(response.getBody().lastName()).isEqualTo("Baker");
        assertThat(response.getBody().email()).isEqualTo("bob@example.com");

        ResponseEntity<UserResponse> meResponse = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(authHeaders), UserResponse.class);
        assertThat(meResponse.getBody().firstName()).isEqualTo("Bob");
        assertThat(meResponse.getBody().lastName()).isEqualTo("Baker");
        assertThat(meResponse.getBody().email()).isEqualTo("bob@example.com");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        HttpHeaders authHeaders = registerAndLogin("pwd-wrong-" + UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me/password", HttpMethod.PUT,
                new HttpEntity<>(new ChangePasswordRequest("not-the-real-password", "newpassword123"), authHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePasswordSucceedsAndOldPasswordNoLongerWorks() {
        String username = "pwd-change-" + UUID.randomUUID();
        HttpHeaders authHeaders = registerAndLogin(username);

        ResponseEntity<Void> changeResponse = restTemplate.exchange(
                "/api/users/me/password", HttpMethod.PUT,
                new HttpEntity<>(new ChangePasswordRequest("password123", "newpassword456"), authHeaders),
                Void.class);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> oldPasswordLogin = restTemplate.postForEntity(
                "/api/auth/login", new AuthRequest(username, "password123"), String.class);
        assertThat(oldPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<UserResponse> newPasswordLogin = restTemplate.postForEntity(
                "/api/auth/login", new AuthRequest(username, "newpassword456"), UserResponse.class);
        assertThat(newPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void profileAndPasswordEndpointsRequireAuthentication() {
        ResponseEntity<String> profileResponse = restTemplate.exchange(
                "/api/users/me", HttpMethod.PATCH, new HttpEntity<>(new UpdateProfileRequest("A", "B", "a@b.com")), String.class);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> passwordResponse = restTemplate.exchange(
                "/api/users/me/password", HttpMethod.PUT, new HttpEntity<>(new ChangePasswordRequest("a", "newpassword123")), String.class);
        assertThat(passwordResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders registerAndLogin(String username) {
        AuthRequest credentials = new AuthRequest(username, "password123");
        restTemplate.postForEntity("/api/auth/register", credentials, Void.class);
        ResponseEntity<Void> loginResponse = restTemplate.postForEntity("/api/auth/login", credentials, Void.class);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        return headers;
    }

    private record AuthRequest(String username, String password) {}

    private record UpdateProfileRequest(String firstName, String lastName, String email) {}

    private record ChangePasswordRequest(String currentPassword, String newPassword) {}

    private record UserResponse(Long id, String username, String firstName, String lastName, String email) {}
}
