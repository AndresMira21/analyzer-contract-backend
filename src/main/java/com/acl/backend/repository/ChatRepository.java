package com.acl.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.backend.model.ChatMessage;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByContractIdOrderByTimestampAsc(String contractId);

    List<ChatMessage> findByUserIdOrderByTimestampDesc(Long userId);

    List<ChatMessage> findByContractIdAndUserIdOrderByTimestampAsc(String contractId, Long userId);

    List<ChatMessage> findByUserIdAndTimestampAfterOrderByTimestampDesc(Long userId, Instant after);

    void deleteByContractId(String contractId);
    void deleteByUserId(Long userId);
    Long countByContractId(String contractId);
    void deleteByConversationId(String conversationId);
}

