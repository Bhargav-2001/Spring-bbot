package com.example.demo.controller;

import com.example.demo.dto.OtpRequest;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @PostMapping("/request-otp")
    public ResponseEntity<String> requestOtp(@RequestBody OtpRequest otpRequest) {
        try {
            userService.requestOtp(otpRequest);
            return ResponseEntity.ok("OTP sent successfully");
        } catch (RuntimeException e) {
            e.printStackTrace(); // Print the full stack trace for debugging
            return ResponseEntity.badRequest().body("Email not found: " + e.getMessage());
        }
    }


    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        try {
            userService.resetPassword(passwordResetRequest);
            return ResponseEntity.ok("Password reset successfully");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid OTP")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP", e);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        }
    }
}
