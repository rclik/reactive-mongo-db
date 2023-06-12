package com.rcelik.springguru.reactivemongodb.mappers;

import org.mapstruct.Mapper;

import com.rcelik.springguru.reactivemongodb.domain.Beer;
import com.rcelik.springguru.reactivemongodb.model.BeerDTO;

@Mapper
public interface BeerMapper {

    Beer beerDTOToBeer(BeerDTO beerDTO);

    BeerDTO beerToBeerDTO(Beer beer);
}
