package com.acme.banking.core.port.in;

import com.acme.banking.core.model.AccountId;
import com.acme.banking.core.model.Money;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.core.model.TransferId;

import java.util.List;

/**
 * Driving port for transfer orchestration use cases.
 *
 * @since 1.0.0
 */
public interface TransferUseCases {

    Transfer initiateTransfer(AccountId sourceAccountId, AccountId targetAccountId, Money amount, String reason);

    Transfer executeTransfer(TransferId transferId);

    Transfer cancelTransfer(TransferId transferId);

    Transfer getTransfer(TransferId id);

    List<Transfer> getTransfersByAccount(AccountId accountId);
}
