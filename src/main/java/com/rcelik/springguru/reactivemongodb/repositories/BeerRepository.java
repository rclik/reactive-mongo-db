package com.rcelik.springguru.reactivemongodb.repositories;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.rcelik.springguru.reactivemongodb.domain.Beer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BeerRepository extends ReactiveMongoRepository<Beer, String> {

    Mono<Beer> findFirstByBeerName(String beerName);

    Flux<Beer> findByBeerStyle(String beerStyle);
}
