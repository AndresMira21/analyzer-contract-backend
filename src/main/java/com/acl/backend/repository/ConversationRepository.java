package com.acl.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.backend.model.Conversation;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}

