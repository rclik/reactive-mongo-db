package com.rcelik.springguru.reactivemongodb.service;

import com.rcelik.springguru.reactivemongodb.model.BeerDTO;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface BeerService {

    Mono<BeerDTO> saveBeer(Mono<BeerDTO> beer);

    Mono<BeerDTO> saveBeer(BeerDTO beerDTO);

    Mono<BeerDTO> getBeer(String beerId);

    Flux<BeerDTO> listBeers();

    Mono<BeerDTO> updateBeer(String id, BeerDTO beerDTO);

    Mono<BeerDTO> patchBeer(String id, BeerDTO beerDTO);

    Mono<Void> deleteBeer(String id);

    Mono<BeerDTO> findFirstBeerByName(String beerName);

    Flux<BeerDTO> findAllByBeerStyle(String beerStyle);

}
