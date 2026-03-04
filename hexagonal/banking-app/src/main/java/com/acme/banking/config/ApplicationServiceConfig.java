package com.acme.banking.config;

import com.acme.banking.core.port.in.AccountUseCases;
import com.acme.banking.core.port.in.CardUseCases;
import com.acme.banking.core.port.in.CustomerUseCases;
import com.acme.banking.core.port.in.TransactionUseCases;
import com.acme.banking.core.port.in.TransferUseCases;
import com.acme.banking.core.port.out.AccountRepository;
import com.acme.banking.core.port.out.CardRepository;
import com.acme.banking.core.port.out.CustomerRepository;
import com.acme.banking.core.port.out.FraudDetection;
import com.acme.banking.core.port.out.NotificationSender;
import com.acme.banking.core.port.out.TransactionRepository;
import com.acme.banking.core.port.out.TransferRepository;
import com.acme.banking.service.application.AccountApplicationService;
import com.acme.banking.service.application.CardApplicationService;
import com.acme.banking.service.application.CustomerApplicationService;
import com.acme.banking.service.application.TransactionApplicationService;
import com.acme.banking.service.application.TransferApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public CustomerUseCases customerUseCases(CustomerRepository customerRepository) {
        return new CustomerApplicationService(customerRepository);
    }

    @Bean
    public AccountUseCases accountUseCases(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository) {
        return new AccountApplicationService(accountRepository, customerRepository, transactionRepository);
    }

    @Bean
    public TransactionUseCases transactionUseCases(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        return new TransactionApplicationService(transactionRepository, accountRepository);
    }

    @Bean
    public CardUseCases cardUseCases(
            CardRepository cardRepository,
            AccountRepository accountRepository,
            NotificationSender notificationSender) {
        return new CardApplicationService(cardRepository, accountRepository, notificationSender);
    }

    @Bean
    public TransferUseCases transferUseCases(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            FraudDetection fraudDetection,
            NotificationSender notificationSender) {
        return new TransferApplicationService(
                transferRepository, accountRepository, transactionRepository, fraudDetection, notificationSender);
    }
}
