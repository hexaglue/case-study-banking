package com.acme.banking.core.port.out;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferId;
import com.acme.banking.core.model.TransferStatus;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for transfer persistence operations.
 *
 * @since 1.0.0
 */
public interface TransferRepository {

    Transfer save(Transfer transfer);

    Optional<Transfer> findById(TransferId id);

    List<Transfer> findBySourceAccountId(AccountId sourceAccountId);

    List<Transfer> findByTargetAccountId(AccountId targetAccountId);

    List<Transfer> findByStatus(TransferStatus status);
}
