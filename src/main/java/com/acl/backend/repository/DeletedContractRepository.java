package com.acl.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.acl.backend.model.DeletedContract;

public interface DeletedContractRepository extends MongoRepository<DeletedContract, String> {
    long countByUserId(Long userId);
}

