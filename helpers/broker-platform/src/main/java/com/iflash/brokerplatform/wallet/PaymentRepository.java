package com.iflash.brokerplatform.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.userId = :userId")
    BigDecimal totalAmount(Long userId);
}
