package com.example.clientragdemo.users.adapter.out.persistence;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

interface UserJdbcRepository extends ListCrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
