package com.raghav.societycrave.service;

import com.raghav.societycrave.entity.User;
import com.raghav.societycrave.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersBySociety(String societyName) {
        return userRepository.findBySocietyNameIgnoreCase(societyName.trim());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email " + email));
    }

    public Page<User> searchUsers(String keyword, int page, int size) {
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, PageRequest.of(page, size));
    }

    public User createUser(User user) {
        if (user.getName() != null) user.setName(user.getName().trim());
        if (user.getEmail() != null) user.setEmail(user.getEmail().trim());
        if (user.getFlatNumber() != null) user.setFlatNumber(user.getFlatNumber().trim());
        if (user.getSocietyName() != null) user.setSocietyName(user.getSocietyName().trim());
        return userRepository.save(user);
    }

    public User updateUser(Long id, User details) {
        User user = getUserById(id);
        if (details.getName() != null) user.setName(details.getName().trim());
        if (details.getEmail() != null) user.setEmail(details.getEmail().trim());
        if (details.getFlatNumber() != null) user.setFlatNumber(details.getFlatNumber().trim());
        if (details.getSocietyName() != null) user.setSocietyName(details.getSocietyName().trim());
        if (details.getPasswordHash() != null && !details.getPasswordHash().isBlank()) user.setPasswordHash(details.getPasswordHash().trim());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id " + id);
        }
        userRepository.deleteById(id);
    }
}
