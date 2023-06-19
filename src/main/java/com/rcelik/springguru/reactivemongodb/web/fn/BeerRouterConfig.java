package com.rcelik.springguru.reactivemongodb.web.fn;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;

/**
 * Holds endpoints and routing logic.
 * calls handler functions directly.
 */
@Configuration
@RequiredArgsConstructor
public class BeerRouterConfig {
    /**
     * endpoints paths
     */
    public static final String BEER_PATH = "/api/v3/beers";
    public static final String BEER_ID = BEER_PATH + "/{beerId}";


    private final BeerHandler beerHandler;

    /**
     * creates a RouterFunction bean that holds configuration for beer endpoints
    */
    @Bean
    RouterFunction<ServerResponse> beerRoutes() {
        return RouterFunctions.route()
                .GET(BEER_PATH, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::listBeers)
                .GET(BEER_ID, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::getBeerById)
                .POST(BEER_PATH, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::createNewBeer)
                .PUT(BEER_ID, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::updateBeer)
                .PATCH(BEER_ID, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::patchBeerById)
                .DELETE(BEER_ID, beerHandler::deleteById)
                .build();
    }
}
