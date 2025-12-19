package com.example.authservice.controller;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.model.User;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.service.MyUserDetailsService;
import com.example.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyUserDetailsService myUserDetailsService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user){
        try {
            User registeredUser = userService.register(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully!");
            response.put("username", registeredUser.getUsername());
            response.put("id",  registeredUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Registration failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest){
        try {
            boolean isValid = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

            if (!isValid) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid credentials");
                response.put("message", "Username or password is incorrect");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            UserDetails userDetails = myUserDetailsService.loadUserByUsername(loginRequest.getUsername());
            String accessToken = jwtTokenProvider.generateToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", loginRequest.getUsername());
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Login failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();

            if (!jwtTokenProvider.validateToken(refreshToken)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = jwtTokenProvider.extractUsername(refreshToken);

            UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);

            String newAccessToken = jwtTokenProvider.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Token refresh failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
