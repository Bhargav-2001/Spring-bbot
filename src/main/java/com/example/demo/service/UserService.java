package com.example.demo.service;

import com.example.demo.dto.OtpRequest;
import com.example.demo.dto.PasswordResetRequest;
import com.example.demo.model.sample;
import com.example.demo.repository.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private String smtpPort;

    @Value("${mail.smtp.username}")
    private String smtpUsername;

    @Value("${mail.smtp.password}")
    private String smtpPassword;

    private final Map<String, String> otpStore = new HashMap<>();

    public void requestOtp(OtpRequest otpRequest) {
        sample user = userRepository.findByEmail(otpRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        otpStore.put(user.getEmail(), otp);

        // Send OTP to user's email
        sendOtpByEmail(user.getEmail(), otp);
    }

    public void resetPassword(PasswordResetRequest passwordResetRequest) {
        String email = passwordResetRequest.getEmail();
        String otp = passwordResetRequest.getOtp();

        if (otpStore.containsKey(email) && otpStore.get(email).equals(otp)) {
            sample user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update user's password
            user.setPassword(passwordEncoder.encode(passwordResetRequest.getNewPassword()));
            userRepository.save(user);

            // Inform user via email that password has been reset
            sendPasswordResetConfirmationEmail(user.getEmail());

            // Remove OTP from store
            otpStore.remove(email);
        } else {
            throw new RuntimeException("Invalid OTP");
        }
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void sendOtpByEmail(String email, String otp) {
        // SMTP server configuration
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        // Create a Session object
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            // Create a MimeMessage object
            Message message = new MimeMessage(session);

            // Set From: header field of the header
            message.setFrom(new InternetAddress(smtpUsername));

            // Set To: header field of the header
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            // Set Subject: header field
            message.setSubject("Your OTP");
            // Now set the actual message
            message.setText("Your OTP is: " + otp);

            // Send message
            Transport.send(message);

            System.out.println("Email sent successfully to " + email);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendPasswordResetConfirmationEmail(String email) {
        // SMTP server configuration
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        // Create a Session object
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            // Create a MimeMessage object
            Message message = new MimeMessage(session);

            // Set From: header field of the header
            message.setFrom(new InternetAddress(smtpUsername));

            // Set To: header field of the header
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

            // Set Subject: header field
            message.setSubject("Password Reset Confirmation");

            // Now set the actual message
            message.setText("Your password has been successfully reset.");

            // Send message
            Transport.send(message);

            System.out.println("Password reset confirmation email sent successfully to " + email);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
