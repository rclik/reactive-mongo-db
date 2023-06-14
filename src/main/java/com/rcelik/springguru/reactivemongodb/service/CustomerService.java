package com.rcelik.springguru.reactivemongodb.service;

import com.rcelik.springguru.reactivemongodb.model.CustomerDTO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Flux<CustomerDTO> getAllCustomers();

    Mono<CustomerDTO> getCustomerById(String id);

    Flux<CustomerDTO> getCustomersByName(String name);

    Mono<CustomerDTO> addCustomer(Mono<CustomerDTO> customer);

    Mono<CustomerDTO> updateCustomer(Mono<CustomerDTO> customer, String customerId);

    Mono<Void> deleteCustomer(String customerId);


}
