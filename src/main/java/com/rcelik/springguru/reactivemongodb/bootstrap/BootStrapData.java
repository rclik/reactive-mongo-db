package com.rcelik.springguru.reactivemongodb.bootstrap;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.rcelik.springguru.reactivemongodb.domain.Beer;
import com.rcelik.springguru.reactivemongodb.repositories.BeerRepository;
import com.rcelik.springguru.reactivemongodb.repositories.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootStrapData implements CommandLineRunner {

    private final BeerRepository beerRepository;
    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        beerRepository.deleteAll().doOnSuccess(success -> {
            loadInitialBeerData();
            log.info("Old data is removed");
        }).subscribe();

        customerRepository.deleteAll().doOnSuccess(success -> {
            log.info("Older customer data is removed.");
        }).subscribe();
    }

    private void loadInitialBeerData() {
        Beer beer1 = Beer.builder()
                .beerName("Space Dust")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantitiyOnHand(12)
                .upc("12121213")
                .build();
        Beer beer2 = Beer.builder()
                .beerName("Efes")
                .beerStyle("IPA")
                .price(new BigDecimal("10.99"))
                .quantitiyOnHand(132)
                .upc("12121213")
                .build();
        Beer beer3 = Beer.builder()
                .beerName("Sunshine City")
                .beerStyle("IPA")
                .price(new BigDecimal("13.99"))
                .quantitiyOnHand(144)
                .upc("12121213")
                .build();

        beerRepository.save(beer1).subscribe();
        beerRepository.save(beer2).subscribe();
        beerRepository.save(beer3).subscribe();
    }
}
