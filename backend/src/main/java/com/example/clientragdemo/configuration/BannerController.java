package com.example.clientragdemo.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Both endpoints must be reachable pre-authentication and pre-acknowledgment (see
// SecurityConfig's permitAll matchers and BannerAckFilter's own exemption for /api/banner*) -
// the banner has to be fetchable before anything else works, and acknowledging it is the
// precondition for everything else, so it can't itself require either.
@RestController
class BannerController {

    private final BannerProperties properties;

    BannerController(BannerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/banner")
    Banner banner(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean acknowledged = session != null && Boolean.TRUE.equals(session.getAttribute(BannerAckFilter.SESSION_ATTRIBUTE));
        return new Banner(this.properties.text(), acknowledged);
    }

    @PostMapping("/api/banner/ack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acknowledge(HttpSession session) {
        session.setAttribute(BannerAckFilter.SESSION_ATTRIBUTE, Boolean.TRUE);
    }

    record Banner(String text, boolean acknowledged) {
    }
}
