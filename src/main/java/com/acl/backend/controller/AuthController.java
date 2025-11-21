package com.acl.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acl.backend.data.AuthData;
import com.acl.backend.model.User;
import com.acl.backend.repository.UserRepository;
import com.acl.backend.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthData.AuthResponse> register(@Valid @RequestBody AuthData.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthData.AuthResponse(token, user.getFullName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthData.AuthResponse> login(@Valid @RequestBody AuthData.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        boolean okCredentials = false;
        try {
            okCredentials = passwordEncoder.matches(req.getPassword(), user.getPassword());
        } catch (Exception ignored) { okCredentials = false; }

        if (!okCredentials) {
            String stored = user.getPassword();
            boolean looksHashed = stored != null && stored.startsWith("$2");
            if (!looksHashed && stored != null && req.getPassword() != null && stored.equals(req.getPassword())) {
                user.setPassword(passwordEncoder.encode(req.getPassword()));
                try { userRepository.save(user); } catch (Exception ignored) {}
                okCredentials = true;
            }
        }

        if (!okCredentials) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthData.AuthResponse(token, user.getFullName(), user.getEmail()));
    }
}
