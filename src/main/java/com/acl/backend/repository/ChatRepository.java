package com.acl.backend.repository;

import java.util.List;
import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.backend.model.ChatMessage;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findContractIdOrderByTimestampAsc(String contractId);

    List<ChatMessage> findByUserIdOrderByTimestampAsc(Long userId);

    List<ChatMessage> findByContractIdAndUserIdOrderByTimestampAsc(String contractId, Long userId);

    List<ChatMessage> findByUserIdAndTimestampAfterOrderByTimestampDesc(Long userId, Instant after);

    void deleteByContractId(String contractId);
    void deleteByUserId(Long userId);
    Long countByContractId(String contractId);
}

