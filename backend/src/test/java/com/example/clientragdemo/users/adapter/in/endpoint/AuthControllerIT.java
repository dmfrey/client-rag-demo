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
        RegisterRequest registration = new RegisterRequest(username, "password123", "Alice", "Anderson", "alice@example.com");

        // BannerAckFilter (see SecurityConfig) gates every non-exempt request, including
        // /api/auth/register and /api/auth/login, until this session has acknowledged the
        // consent banner - acknowledge first and carry that session's cookie through every call
        // below, the same way a real browser persists a session cookie across a request
        // sequence (TestRestTemplate doesn't do this automatically, unlike a browser).
        ResponseEntity<Void> ackResponse = restTemplate.postForEntity("/api/banner/ack", null, Void.class);
        assertThat(ackResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String bannerCookie = ackResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(bannerCookie).isNotNull();
        HttpEntity<RegisterRequest> registerRequest = new HttpEntity<>(registration, cookieHeaders(bannerCookie));

        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, UserResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().id()).isNotNull();
        assertThat(registerResponse.getBody().username()).isEqualTo(username);
        assertThat(registerResponse.getBody().firstName()).isEqualTo("Alice");
        assertThat(registerResponse.getBody().lastName()).isEqualTo("Anderson");
        assertThat(registerResponse.getBody().email()).isEqualTo("alice@example.com");

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, String.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> unauthenticatedMe = restTemplate.getForEntity("/api/auth/me", String.class);
        assertThat(unauthenticatedMe.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpEntity<AuthRequest> badLoginRequest = new HttpEntity<>(new AuthRequest(username, "wrong-password"), cookieHeaders(bannerCookie));
        ResponseEntity<String> badLogin = restTemplate.postForEntity("/api/auth/login", badLoginRequest, String.class);
        assertThat(badLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpEntity<AuthRequest> loginRequest = new HttpEntity<>(credentials, cookieHeaders(bannerCookie));
        ResponseEntity<UserResponse> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, UserResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().username()).isEqualTo(username);

        // Login doesn't always issue a new Set-Cookie - only when Spring Security's
        // session-fixation protection actually changes the session ID, which it doesn't do on
        // every request. Reuse the banner-ack cookie in that case; either way the banner-ack
        // attribute is present on whichever session ID is now current.
        String loginCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String sessionCookie = (loginCookie != null) ? loginCookie : bannerCookie;

        HttpEntity<Void> authenticatedRequest = new HttpEntity<>(cookieHeaders(sessionCookie));
        ResponseEntity<UserResponse> meResponse = restTemplate.exchange("/api/auth/me", HttpMethod.GET, authenticatedRequest, UserResponse.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().username()).isEqualTo(username);
        assertThat(meResponse.getBody().firstName()).isEqualTo("Alice");
        assertThat(meResponse.getBody().lastName()).isEqualTo("Anderson");
        assertThat(meResponse.getBody().email()).isEqualTo("alice@example.com");

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

    private record RegisterRequest(String username, String password, String firstName, String lastName, String email) {}

    private record UserResponse(Long id, String username, String firstName, String lastName, String email) {}
}
