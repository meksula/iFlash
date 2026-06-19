package com.iflash.brokerplatform.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Login == registration: returns the existing user or creates one on first sight. */
    @Transactional
    public User loginOrRegister(String rawEmail) {
        String email = normalize(rawEmail);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        return userRepository.findByEmail(email)
                             .orElseGet(() -> userRepository.save(new User(email)));
    }

    @Transactional(readOnly = true)
    public User require(Long id) {
        return userRepository.findById(id)
                             .orElseThrow(() -> new IllegalStateException("User not found: " + id));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
