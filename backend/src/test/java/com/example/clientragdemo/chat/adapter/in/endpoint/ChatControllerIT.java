package com.example.clientragdemo.chat.adapter.in.endpoint;

import com.example.clientragdemo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ChatControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void sendMessageStreamsGroundedAnswerWithSourcesAndAutoTitle() {
        HttpHeaders authHeaders = registerAndLogin("chat-user-" + UUID.randomUUID());

        String filename = "facts-" + UUID.randomUUID() + ".txt";
        uploadAndAwaitReady(authHeaders, filename, "The secret code word for this test is BANANAFISH42.");

        Long sessionId = createChatSession(authHeaders);

        HttpHeaders sseHeaders = new HttpHeaders();
        sseHeaders.addAll(authHeaders);
        sseHeaders.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));

        HttpEntity<SendMessageRequest> request = new HttpEntity<>(new SendMessageRequest("What is the secret code word?"), sseHeaders);
        ResponseEntity<String> streamResponse = restTemplate.exchange(
                "/api/chats/" + sessionId + "/messages", HttpMethod.POST, request, String.class);

        assertThat(streamResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = streamResponse.getBody();
        assertThat(body).isNotBlank();
        assertThat(body).contains("event:token");
        assertThat(body).contains("event:sources");
        assertThat(body).contains(filename);

        ResponseEntity<MessageResponse[]> messagesResponse = restTemplate.exchange(
                "/api/chats/" + sessionId + "/messages", HttpMethod.GET, new HttpEntity<>(authHeaders), MessageResponse[].class);
        assertThat(messagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(messagesResponse.getBody())).extracting(MessageResponse::role).contains("USER", "ASSISTANT");

        await().atMost(Duration.ofSeconds(30)).until(
                () -> restTemplate.exchange("/api/chats", HttpMethod.GET, new HttpEntity<>(authHeaders), SessionResponse[].class).getBody(),
                sessions -> sessions.length == 1 && sessions[0].title() != null);
    }

    @Test
    void messagesRequireOwnership() {
        HttpHeaders ownerHeaders = registerAndLogin("chat-owner-" + UUID.randomUUID());
        Long sessionId = createChatSession(ownerHeaders);

        HttpHeaders otherHeaders = registerAndLogin("chat-intruder-" + UUID.randomUUID());
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chats/" + sessionId + "/messages", HttpMethod.GET, new HttpEntity<>(otherHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void nonexistentSessionReturnsNotFound() {
        HttpHeaders headers = registerAndLogin("chat-none-" + UUID.randomUUID());
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chats/999999999/messages", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long createChatSession(HttpHeaders authHeaders) {
        ResponseEntity<SessionResponse> response = restTemplate.postForEntity(
                "/api/chats", new HttpEntity<>(null, authHeaders), SessionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private void uploadAndAwaitReady(HttpHeaders authHeaders, String filename, String content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", namedResource(filename, content.getBytes(StandardCharsets.UTF_8)));

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(authHeaders);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<UploadResponse> response = restTemplate.postForEntity("/api/documents", new HttpEntity<>(body, headers), UploadResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Long documentId = response.getBody().id();

        await().atMost(Duration.ofSeconds(60)).until(
                () -> restTemplate.exchange("/api/documents/" + documentId, HttpMethod.GET, new HttpEntity<>(authHeaders), UploadResponse.class).getBody(),
                document -> !"PROCESSING".equals(document.status()));
    }

    private HttpHeaders registerAndLogin(String username) {
        AuthRequest credentials = new AuthRequest(username, "password123");
        restTemplate.postForEntity("/api/auth/register", credentials, Void.class);
        ResponseEntity<Void> loginResponse = restTemplate.postForEntity("/api/auth/login", credentials, Void.class);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        return headers;
    }

    private static ByteArrayResource namedResource(String filename, byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private record AuthRequest(String username, String password) {}

    private record SendMessageRequest(String content) {}

    private record SessionResponse(Long id, String title, String createdAt, String updatedAt) {}

    private record MessageResponse(String role, String content) {}

    private record UploadResponse(Long id, String filename, String contentType, String status, String errorMessage,
                                   Integer chunkCount, String uploadedBy, String createdAt, String updatedAt) {}
}
