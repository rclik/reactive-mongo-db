package com.rcelik.springguru.reactivemongodb.web.fn;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.rcelik.springguru.reactivemongodb.model.BeerDTO;
import com.rcelik.springguru.reactivemongodb.service.BeerService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Handles coming requests from Router.
 * Calls service methods by getting parameters from server requests and returns
 * server response.
 * Modifies server response for customer.
 */
@Component
@RequiredArgsConstructor
public class BeerHandler {
    private final BeerService beerService;

    public Mono<ServerResponse> listBeers(ServerRequest request) {
        // it returns all elements on a ServerResponse object
        return ServerResponse.ok().body(beerService.listBeers(), BeerDTO.class);
    }

    public Mono<ServerResponse> getBeerById(ServerRequest request) {

        
        return ServerResponse.ok().body(
                beerService.getBeer(request.pathVariable("beerId"))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))),
                BeerDTO.class);
         
        /* 
        return beerService.getBeer(request.pathVariable("beerId"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(beerDto -> ServerResponse.ok().bodyValue(beerDto));
                */
    }

    public Mono<ServerResponse> createNewBeer(ServerRequest request) {
        return beerService.saveBeer(request.bodyToMono(BeerDTO.class))
                .flatMap(beerDTO -> ServerResponse
                        .created(UriComponentsBuilder.fromPath(BeerRouterConfig.BEER_ID).build(beerDTO.getId()))
                        .build());
    }

    public Mono<ServerResponse> updateBeer(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .flatMap(beerDTO -> {
                    return beerService.updateBeer(request.pathVariable("beerId"), beerDTO);
                }).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(savedDto -> {
                    return ServerResponse.noContent().build();
                });
    }

    public Mono<ServerResponse> patchBeerById(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .flatMap(requestedDto -> beerService.patchBeer(request.pathVariable("beerId"), requestedDto))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(patchedDto -> ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> deleteById(ServerRequest request) {
        return beerService.getBeer(request.pathVariable("beerId"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(foundBeerDto -> beerService.deleteBeer(foundBeerDto.getId()))
                .then(ServerResponse.noContent().build());
    }
}
