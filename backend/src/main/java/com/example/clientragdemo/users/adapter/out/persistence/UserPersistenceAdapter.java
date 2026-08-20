package com.example.clientragdemo.users.adapter.out.persistence;

import com.example.clientragdemo.users.application.domain.model.User;
import com.example.clientragdemo.users.application.port.out.LoadUserByUsernamePort;
import com.example.clientragdemo.users.application.port.out.SaveUserPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements SaveUserPort, LoadUserByUsernamePort {

    private final UserJdbcRepository repository;

    UserPersistenceAdapter(UserJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserEntity saved = repository.save(new UserEntity(user.id(), user.username(), user.passwordHash(), user.createdAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<User> loadByUsername(String username) {
        return repository.findByUsername(username).map(UserPersistenceAdapter::toDomain);
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.id(), entity.username(), entity.passwordHash(), entity.createdAt());
    }
}
