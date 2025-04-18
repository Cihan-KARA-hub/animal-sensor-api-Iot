package com.yelman.cloudserver.services;

import com.yelman.cloudserver.model.Users;
import com.yelman.cloudserver.repository.UserRepository;
import com.yelman.cloudserver.services.impl.UserServicesImpl;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServices implements UserServicesImpl {

    private final UserRepository userRepository;

    public UserServices(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean registerUser(Users user) {
        Users s = userRepository.save(user);
        return s.getId() != null;
    }

    @Override
    public Users getUserByUsername(String username) {
        return null;
    }

    @Override
    public Users getUserByEmail(String email) {
        Optional<Users> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    @Override
    public Users getUserById(Long id) {
        Optional<Users> user = userRepository.findById(id);
        return user.orElse(null);
    }

    @Override
    public void updateUser(Users user) {

    }

    @Override
    public void deleteUser(Long id) {

    }
}
