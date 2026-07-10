package com.example.meeting.controller;

import com.example.meeting.dto.ApiResponse;
import com.example.meeting.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        Object userId = request.get("userId");
        Object username = request.get("username");
        String login = userId == null || String.valueOf(userId).trim().length() == 0 ? String.valueOf(username) : String.valueOf(userId);
        return ApiResponse.ok(authService.login(login));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(currentUser);
    }
}
