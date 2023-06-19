package com.rcelik.springguru.reactivemongodb.repositories;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.rcelik.springguru.reactivemongodb.domain.Customer;

import reactor.core.publisher.Flux;

public interface CustomerRepository extends ReactiveMongoRepository<Customer, String> {
    Flux<Customer> findByName(String name);
}
