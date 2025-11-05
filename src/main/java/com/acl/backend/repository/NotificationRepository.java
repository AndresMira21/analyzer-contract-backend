package com.acl.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadTrueOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    List<Notification> findByContractIdOrderByCreatedAtDesc(String contractId);

    long countByUserIdAndReadFalse(Long userId);

    void deleteByUserIdAndCreatedAtBefore(Long userId, Instant before);
}