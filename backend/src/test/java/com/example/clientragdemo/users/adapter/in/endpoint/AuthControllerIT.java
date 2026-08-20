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
class AuthControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerLoginMeLogoutFlow() {
        String username = "alice-" + UUID.randomUUID();
        AuthRequest credentials = new AuthRequest(username, "password123");

        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", credentials, UserResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().id()).isNotNull();
        assertThat(registerResponse.getBody().username()).isEqualTo(username);

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity("/api/auth/register", credentials, String.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> unauthenticatedMe = restTemplate.getForEntity("/api/auth/me", String.class);
        assertThat(unauthenticatedMe.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> badLogin = restTemplate.postForEntity("/api/auth/login", new AuthRequest(username, "wrong-password"), String.class);
        assertThat(badLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<UserResponse> loginResponse = restTemplate.postForEntity("/api/auth/login", credentials, UserResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().username()).isEqualTo(username);

        String sessionCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).isNotNull();

        HttpEntity<Void> authenticatedRequest = new HttpEntity<>(cookieHeaders(sessionCookie));
        ResponseEntity<UserResponse> meResponse = restTemplate.exchange("/api/auth/me", HttpMethod.GET, authenticatedRequest, UserResponse.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().username()).isEqualTo(username);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange("/api/auth/logout", HttpMethod.POST, authenticatedRequest, Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> meAfterLogout = restTemplate.exchange("/api/auth/me", HttpMethod.GET, authenticatedRequest, String.class);
        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static HttpHeaders cookieHeaders(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookie);
        return headers;
    }

    private record AuthRequest(String username, String password) {}

    private record UserResponse(Long id, String username) {}
}
