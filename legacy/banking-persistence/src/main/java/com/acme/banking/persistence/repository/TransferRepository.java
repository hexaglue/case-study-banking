package com.acme.banking.persistence.repository;

import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findBySourceAccountId(Long sourceAccountId);

    List<Transfer> findByTargetAccountId(Long targetAccountId);

    List<Transfer> findByStatus(TransferStatus status);
}
