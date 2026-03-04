package com.acme.banking.api.controller;

import com.acme.banking.api.dto.CreateCustomerRequest;
import com.acme.banking.api.dto.CustomerResponse;
import com.acme.banking.core.model.Customer;
import com.acme.banking.service.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone()
        );
        return CustomerResponse.from(customer);
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        Customer customer = customerService.getCustomer(id);
        return CustomerResponse.from(customer);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(
            id,
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone()
        );
        return CustomerResponse.from(customer);
    }
}
