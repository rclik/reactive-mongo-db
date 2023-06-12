package com.rcelik.springguru.reactivemongodb.web.fn;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import com.rcelik.springguru.reactivemongodb.model.BeerDTO;
import com.rcelik.springguru.reactivemongodb.service.BeerServiceImpl;
import com.rcelik.springguru.reactivemongodb.service.BeerServiceImplTest;

import reactor.core.publisher.Mono;

@SpringBootTest // used to initialize Spring Context and run that tests on it
@AutoConfigureWebTestClient // configuring WebTestClient so that it can be accessible on that class
public class BeerEndpointTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("All beers should be returned")
    void testListBeers() {
        webTestClient.get().uri(BeerRouterConfig.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.size()", 3);
    }

    @Test
    @DisplayName("Should create new beer")
    void testCreateBeer() {
        BeerDTO beerDto = BeerServiceImplTest.genereateTestBeerDTO();

        webTestClient.post()
                        .uri(BeerRouterConfig.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(beerDto), BeerDTO.class)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().exists(HttpHeaders.LOCATION);
    }

    @Test
    @DisplayName("Should get created beer")
    void testGetBeerById() {
        BeerDTO beerDto = BeerServiceImplTest.genereateTestBeerDTO();

        webTestClient.post()
                        .uri(BeerRouterConfig.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(beerDto), BeerDTO.class)
                        .exchange().expectHeader().value(HttpHeaders.LOCATION, locationHeaderValue -> {
                            String beerId = locationHeaderValue.substring(locationHeaderValue.lastIndexOf("/") + 1);
                            webTestClient.get()
                                            .uri(BeerRouterConfig.BEER_ID, beerId)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .exchange()
                                            .expectStatus().isOk()
                                            .expectBody().jsonPath("$.id", beerDto.getId());
                        });
    }

}
