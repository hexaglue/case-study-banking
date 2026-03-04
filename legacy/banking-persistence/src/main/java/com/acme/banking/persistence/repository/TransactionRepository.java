package com.acme.banking.persistence.repository;

import com.acme.banking.core.model.Transaction;
import com.acme.banking.core.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndType(Long accountId, TransactionType type);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
