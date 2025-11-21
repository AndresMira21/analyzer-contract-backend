package com.acl.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acl.backend.model.Conversation;
import com.acl.backend.model.User;
import com.acl.backend.repository.ChatRepository;
import com.acl.backend.repository.ConversationRepository;
import com.acl.backend.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/chats")
public class ChatsController {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public ChatsController(ConversationRepository conversationRepository,
                           UserRepository userRepository,
                           ChatRepository chatRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    @GetMapping
    public ResponseEntity<List<Conversation>> list(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<Conversation> list = conversationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(list);
    }

    public static class CreateRequest {
        @NotBlank
        public String title;
        public Instant date;
    }

    @PostMapping
    public ResponseEntity<Conversation> create(@RequestBody CreateRequest req,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Conversation c = new Conversation();
        c.setUserId(user.getId());
        c.setTitle(req.title);
        c.setCreatedAt(req.date != null ? req.date : Instant.now());
        Conversation saved = conversationRepository.save(c);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Conversation c = conversationRepository.findById(id).orElse(null);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        if (c.getUserId() != null && !c.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        conversationRepository.deleteById(id);
        try { chatRepository.deleteByConversationId(id); } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }
}

