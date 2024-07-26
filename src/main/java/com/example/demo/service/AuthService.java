package com.example.demo.service;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.sample;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public void register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        String username = registerRequest.getUsername();

//        if (userRepository.findByUsername(registerRequest.getUsername())) {
//            throw new RuntimeException("Username already exists: " + username);
//        }
        if (!email.endsWith("@gmail.com")) {
            throw new IllegalArgumentException("Email must end with @gmail.com");

        }
        sample user = new sample();
        user.setUsername(registerRequest.getUsername());

        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userRepository.save(user);
    }

    public JwtResponse login(LoginRequest loginRequest) {
        // Find user by username
        sample user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if passwords match
        if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUsername());
            // Return token only
            return new JwtResponse(token);
        } else {
            throw new RuntimeException("Invalid credentials");
        }

    }

    public String validateTokenAndGetMessage(String token) {
        // Validate token
        String username = jwtUtil.extractUsername(token);
        if (username != null && jwtUtil.validateToken(token, username)) {
            return "Login successful";
        } else {
            throw new RuntimeException("Failed to authorize token");
        }
    }
}
