package com.example.clientragdemo.users.application.port.out;

import com.example.clientragdemo.users.application.domain.model.User;

public interface SaveUserPort {

    User save(User user);
}
