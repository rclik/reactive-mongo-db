package com.rcelik.springguru.reactivemongodb.mappers;

import org.mapstruct.Mapper;

import com.rcelik.springguru.reactivemongodb.domain.Customer;
import com.rcelik.springguru.reactivemongodb.model.CustomerDTO;

@Mapper
public interface CustomerMapper {

    CustomerDTO customerToCustomerDto(Customer customer);

    Customer customerDtoToCustomer(CustomerDTO customerDto);
}
