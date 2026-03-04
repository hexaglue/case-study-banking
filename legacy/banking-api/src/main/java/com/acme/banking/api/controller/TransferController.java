package com.acme.banking.api.controller;

import com.acme.banking.api.dto.TransferRequest;
import com.acme.banking.api.dto.TransferResponse;
import com.acme.banking.core.model.Transfer;
import com.acme.banking.service.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse initiateTransfer(@Valid @RequestBody TransferRequest request) {
        Transfer transfer = transferService.initiateTransfer(
            request.sourceAccountId(),
            request.targetAccountId(),
            request.amount(),
            request.reason()
        );
        return TransferResponse.from(transfer);
    }

    @PostMapping("/{id}/execute")
    public TransferResponse executeTransfer(@PathVariable Long id) {
        Transfer transfer = transferService.executeTransfer(id);
        return TransferResponse.from(transfer);
    }

    @PostMapping("/{id}/cancel")
    public TransferResponse cancelTransfer(@PathVariable Long id) {
        Transfer transfer = transferService.cancelTransfer(id);
        return TransferResponse.from(transfer);
    }

    @GetMapping("/{id}")
    public TransferResponse getTransfer(@PathVariable Long id) {
        Transfer transfer = transferService.getTransfer(id);
        return TransferResponse.from(transfer);
    }
}
