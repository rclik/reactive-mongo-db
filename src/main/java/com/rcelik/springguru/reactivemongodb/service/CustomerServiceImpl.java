package com.rcelik.springguru.reactivemongodb.service;

import org.springframework.stereotype.Service;

import com.rcelik.springguru.reactivemongodb.mappers.CustomerMapper;
import com.rcelik.springguru.reactivemongodb.model.CustomerDTO;
import com.rcelik.springguru.reactivemongodb.repositories.CustomerRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Override
    public Flux<CustomerDTO> getAllCustomers() {
        return repository.findAll()
                .map(mapper::customerToCustomerDto);
    }

    @Override
    public Mono<CustomerDTO> getCustomerById(String id) {
        return repository.findById(id)
                .map(mapper::customerToCustomerDto);
    }

    @Override
    public Flux<CustomerDTO> getCustomersByName(String name) {
        return repository.findByName(name).map(mapper::customerToCustomerDto);
    }

    @Override
    public Mono<CustomerDTO> addCustomer(Mono<CustomerDTO> customer) {
        return customer.map(mapper::customerDtoToCustomer)
                .flatMap(repository::save)
                .map(mapper::customerToCustomerDto);
    }

    @Override
    public Mono<CustomerDTO> updateCustomer(CustomerDTO customer, String customerId) {
        return repository.findById(customerId).map(foudnCustomer -> {
            foudnCustomer.setName(customer.getName());
            return foudnCustomer;
        }).map(mapper::customerToCustomerDto);
    }

    @Override
    public Mono<Void> deleteCustomer(String customerId) {
        return repository.deleteById(customerId);
    }

}
