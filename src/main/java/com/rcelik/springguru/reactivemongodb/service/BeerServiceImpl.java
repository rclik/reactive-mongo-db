package com.rcelik.springguru.reactivemongodb.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rcelik.springguru.reactivemongodb.mappers.BeerMapper;
import com.rcelik.springguru.reactivemongodb.model.BeerDTO;
import com.rcelik.springguru.reactivemongodb.repositories.BeerRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BeerServiceImpl implements BeerService {
    private final BeerMapper beerMapper;
    private final BeerRepository beerRepository;

    @Override
    public Mono<BeerDTO> saveBeer(Mono<BeerDTO> beer) {
        return beer.map(beerMapper::beerDTOToBeer)
                .flatMap(beerRepository::save)
                .map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Mono<BeerDTO> getBeer(String beerId) {
        return beerRepository.findById(beerId).map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Mono<BeerDTO> saveBeer(BeerDTO beerDTO) {
        return beerRepository.save(beerMapper.beerDTOToBeer(beerDTO)).map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Flux<BeerDTO> listBeers() {
        return beerRepository.findAll().map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Mono<BeerDTO> updateBeer(String id, BeerDTO beerDTO) {
        return beerRepository.findById(id)
                .map(foundBeer -> {
                    foundBeer.setBeerName(beerDTO.getBeerName());
                    foundBeer.setBeerStyle(beerDTO.getBeerStyle());
                    foundBeer.setPrice(beerDTO.getPrice());
                    foundBeer.setQuantitiyOnHand(beerDTO.getQuantitiyOnHand());
                    foundBeer.setUpc(beerDTO.getUpc());
                    return foundBeer;
                }) // updating found beer with new one
                .flatMap(beerRepository::save) // wrapping Beer with Mono then saving it to database
                .map(beerMapper::beerToBeerDTO); // mapping Mono<Beer> to Mono<BeerDTO>
    }

    @Override
    public Mono<BeerDTO> patchBeer(String id, BeerDTO beerDTO) {
        return beerRepository.findById(id)
                .map(foundBeer -> {
                    if (StringUtils.hasText(beerDTO.getBeerName())) {
                        foundBeer.setBeerName(beerDTO.getBeerName());
                    }
                    if (StringUtils.hasText(beerDTO.getBeerStyle())) {
                        foundBeer.setBeerStyle(beerDTO.getBeerStyle());
                    }
                    if (StringUtils.hasText(beerDTO.getUpc())) {
                        foundBeer.setUpc(beerDTO.getUpc());
                    }
                    if (beerDTO.getPrice() != null) {
                        foundBeer.setPrice(beerDTO.getPrice());
                    }
                    if (beerDTO.getQuantitiyOnHand() != null) {
                        foundBeer.setQuantitiyOnHand(beerDTO.getQuantitiyOnHand());
                    }
                    return foundBeer;
                }).flatMap(beerRepository::save)
                .map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Mono<Void> deleteBeer(String id) {
        return beerRepository.deleteById(id);
    }

    @Override
    public Mono<BeerDTO> findFirstBeerByName(String beerName) {
        return beerRepository.findFirstByBeerName(beerName).map(beerMapper::beerToBeerDTO);
    }

    @Override
    public Flux<BeerDTO> findAllByBeerStyle(String beerStyle) {
        return beerRepository.findByBeerStyle(beerStyle)
                .map(beerMapper::beerToBeerDTO);
    }
}
