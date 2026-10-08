package com.lankaautoparts.autosparepro.user.service;

import com.lankaautoparts.autosparepro.user.model.Role;
import com.lankaautoparts.autosparepro.user.model.User;
import com.lankaautoparts.autosparepro.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void createUser(String username, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * Updates only the role of an existing user. Returns true if the user
     * existed and was updated, false otherwise. Callers are responsible for
     * business-rule checks (e.g. not letting an admin demote themselves).
     */
    public boolean updateRole(Long id, Role role) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setRole(role);
                    userRepository.save(u);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Verifies the current password and, if it matches, updates the user's
     * password to the new one (BCrypt-encoded). Returns true on success,
     * false if the current password was wrong or the user doesn't exist.
     */
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(currentPassword, u.getPassword()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(u);
                    return true;
                })
                .orElse(false);
    }
}
