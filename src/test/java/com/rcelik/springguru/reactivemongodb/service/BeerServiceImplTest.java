package com.rcelik.springguru.reactivemongodb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.rcelik.springguru.reactivemongodb.domain.Beer;
import com.rcelik.springguru.reactivemongodb.mappers.BeerMapper;
import com.rcelik.springguru.reactivemongodb.mappers.BeerMapperImpl;
import com.rcelik.springguru.reactivemongodb.model.BeerDTO;

import reactor.core.publisher.Mono;

@SpringBootTest
public class BeerServiceImplTest {

    @Autowired
    private BeerService beerService;

    @Autowired
    private BeerMapper beerMapper;

    private BeerDTO beerDTO;

    @BeforeEach
    void setUp() {
        beerDTO = beerMapper.beerToBeerDTO(generateTestBeer());
    }

    @Test
    @DisplayName("Test saveBeer using subsciber")
    void testSaveBeer() {
        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        AtomicBoolean finished = new AtomicBoolean(false);
        // we use AtomicReference here to check everything is fine.
        // for example if there exists an error, that passes atomicBoolean to be set as
        // true but it does not succeed
        AtomicReference<BeerDTO> atomicDto = new AtomicReference<BeerDTO>();

        savedMono.subscribe(
                savedDto -> {
                    System.out.println("saved beer id: %s".formatted(savedDto.getId()));
                    finished.set(true);
                    atomicDto.set(savedDto);
                });

        Awaitility.await().untilTrue(finished);

        BeerDTO savedBeer = atomicDto.get();
        assertNotNull(savedBeer, "beer should not be null");
        assertNotNull(savedBeer.getId(), "saved object should have not null id");
    }

    @Test
    @DisplayName("Test saveBeer using block")
    void testSaveBeerWithBlock() {
        // this way does same thing with subscriber but it has less code
        BeerDTO savedBeer = beerService.saveBeer(Mono.just(beerDTO)).block();
        assertNotNull(savedBeer, "beer should not be null");
        assertNotNull(savedBeer.getId(), "saved object should have not null id");
        // in normal code we should not use block
    }

    @Test
    @DisplayName("updateBeer with blocking manner")
    void testUpdateBeerWithBlockingManner() {

        final String newName = "new beer name";
        BeerDTO savedBeerDTO = getSavedBeerDTO();
        savedBeerDTO.setBeerName(newName);

        BeerDTO updatedBeerDTO = beerService.updateBeer(savedBeerDTO.getId(), savedBeerDTO).block();
        assertEquals(savedBeerDTO.getId(), updatedBeerDTO.getId(), "beer id should not be changed");

        BeerDTO fetchBeerDTO = beerService.getBeer(savedBeerDTO.getId()).block();
        assertEquals(fetchBeerDTO.getBeerName(), newName, "beer name should be updated");
    }

    @Test
    @DisplayName("updateBeer with subscriber way")
    void testUpdateBeerWithSubscriber() {
        final String newName = "new beer name subscriber way";
        BeerDTO savedBeerDTO = getSavedBeerDTO();
        savedBeerDTO.setBeerName(newName);

        AtomicReference<BeerDTO> updatedBeerReference = new AtomicReference<BeerDTO>();
        beerService.updateBeer(savedBeerDTO.getId(), savedBeerDTO).subscribe(
                updatedBeerDto -> {
                    updatedBeerReference.set(updatedBeerDto);
                });

        // saying that wait until updating object is finished
        Awaitility.await().until(() -> updatedBeerReference.get() != null);
        assertEquals(newName, updatedBeerReference.get().getBeerName());
    }

    @Test
    @DisplayName("deleteBeer with blocking manner")
    void testDeleteBeerWithBlockingManner() {
        BeerDTO savedDto = getSavedBeerDTO();

        String id = savedDto.getId();

        // needed to be deleted from database
        beerService.deleteBeer(id).block();

        BeerDTO foundBeer = beerService.getBeer(id).block();
        assertNull(foundBeer, "beer should be deleted");
    }

    @Test
    @DisplayName("deleteBeer with subscriber manner")
    void testDeleteBeerWithSubscriberManner() {
        BeerDTO savedDto = getSavedBeerDTO();

        AtomicBoolean finished = new AtomicBoolean(false);
        String id = savedDto.getId();

        // needed to be deleted from database
        beerService.deleteBeer(id).then(Mono.fromRunnable(() -> finished.set(true))).subscribe();

        Awaitility.await().untilTrue(finished);

        BeerDTO foundBeer = beerService.getBeer(id).block();
        assertNull(foundBeer, "beer should be deleted");
    }

    @Test
    @DisplayName("should return first beer whose name is given")
    void testFindFirstBeerByNameWithSubscriberWay() {
        BeerDTO savedBeer = getSavedBeerDTO();
        String savedBeerName = savedBeer.getBeerName();

        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<BeerDTO> atomicReference = new AtomicReference<BeerDTO>();

        beerService.findFirstBeerByName(savedBeerName).subscribe(
                foundBeer -> {
                    finished.set(true);
                    atomicReference.set(foundBeer);
                });

        Awaitility.await().untilTrue(finished);
        BeerDTO foundBeer = atomicReference.get();

        assertNotNull(foundBeer, "beer should be found");
        assertEquals(savedBeerName, foundBeer.getBeerName(), "beer name should be same as requested name");
    }

    @Test
    @DisplayName("should return first beer whose name is given")
    void testFindFirstBeerByNameWithBlocingWay() {
        BeerDTO savedBeer = getSavedBeerDTO();
        String savedBeerName = savedBeer.getBeerName();

        BeerDTO foundBeer = beerService.findFirstBeerByName(savedBeerName).block();

        assertNotNull(foundBeer, "beer should be found");
        assertEquals(savedBeerName, foundBeer.getBeerName(), "beer name should be same as requested name");
    }

    @Test
    @DisplayName("should patch the part of the domain object")
    void testPatchBeerWithBlockingWay() {
        BeerDTO savedBeer = getSavedBeerDTO();

        final BigDecimal savedBeerPrice = savedBeer.getPrice();
        final String id = savedBeer.getId();

        // now need to patch some of its parts
        BeerDTO patchedObject = BeerDTO.builder().beerName("new beer name").build();

        beerService.patchBeer(id, patchedObject).block();

        BeerDTO foundBeer = beerService.getBeer(id).block();
        assertNotNull(foundBeer, "beer should be found");
        assertEquals(foundBeer.getBeerName(), patchedObject.getBeerName(), "name should change");
        assertEquals(foundBeer.getPrice(), savedBeerPrice, "price should not change");
    }

    @Test
    @DisplayName("should patch the part of the domain object")
    void testPatchBeerWithSubscriberWay() {
        BeerDTO savedBeer = getSavedBeerDTO();

        final BigDecimal savedBeerPrice = savedBeer.getPrice();
        final String id = savedBeer.getId();

        // now need to patch some of its parts
        BeerDTO patchedObject = BeerDTO.builder().beerName("new beer name").build();

        AtomicReference<BeerDTO> loadingBeerDto = new AtomicReference<BeerDTO>();

        beerService.patchBeer(id, patchedObject).subscribe(
            updatedBeer -> {
                loadingBeerDto.set(updatedBeer);
            }
        );

        Awaitility.await().until( () -> loadingBeerDto.get() != null);


        BeerDTO foundBeer = beerService.getBeer(id).block();
        assertNotNull(foundBeer, "beer should be found");
        assertEquals(foundBeer.getBeerName(), patchedObject.getBeerName(), "name should change");
        assertEquals(foundBeer.getPrice(), savedBeerPrice, "price should not change");
    }

    @Test
    @DisplayName("should return all records whose beer style is provided one")
    void testFindAllByBeerStyleWithSubscriverWay(){
        BeerDTO savedBeer = getSavedBeerDTO();

        AtomicBoolean isDataLoaded = new AtomicBoolean(false);
        AtomicReference<BeerDTO> retunedBeersRef = new AtomicReference<BeerDTO>();

        beerService.findAllByBeerStyle(savedBeer.getBeerStyle()).subscribe(
            allBeers -> {
                isDataLoaded.set(true);
                retunedBeersRef.set(allBeers);
                System.out.println(allBeers.toString());
            }
        );

        Awaitility.await().untilTrue(isDataLoaded);
        
        BeerDTO returnedBeers = retunedBeersRef.get();

        assertNotNull(returnedBeers, "returned beer should not be null");
    }

    public static Beer generateTestBeer() {
        return Beer.builder()
                .beerName("Space Dusty")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantitiyOnHand(12)
                .build();
    }

    public static BeerDTO genereateTestBeerDTO() {
        // BeerMapperImpl is generated class
        return new BeerMapperImpl().beerToBeerDTO(generateTestBeer());
    }

    private BeerDTO getSavedBeerDTO() {
        return beerService.saveBeer(Mono.just(genereateTestBeerDTO())).block();
    }
}
