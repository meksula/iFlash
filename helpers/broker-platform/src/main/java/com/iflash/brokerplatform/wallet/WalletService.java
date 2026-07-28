package com.iflash.brokerplatform.wallet;

import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public WalletService(UserRepository userRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    /** Simulated payment gateway: credits the user's account by the given amount. */
    @Transactional
    public void topUp(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than 0");
        }
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setBalance(user.getBalance().add(amount));
        paymentRepository.save(new Payment(userId, amount));
    }

    @Transactional(readOnly = true)
    public List<Payment> history(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
