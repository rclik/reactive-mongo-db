package com.rcelik.springguru.reactivemongodb.web.fn;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.rcelik.springguru.reactivemongodb.model.CustomerDTO;
import com.rcelik.springguru.reactivemongodb.service.CustomerService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerHandler {

    private final CustomerService customerService;

    public Mono<ServerResponse> getAllCustomers(ServerRequest request) {
        return ServerResponse.ok().body(customerService.getAllCustomers(), CustomerDTO.class);
    }

    public Mono<ServerResponse> getCustomerById(ServerRequest request) {
        return customerService.getCustomerById(request.pathVariable("id"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(customerDto -> ServerResponse.ok().body(customerDto, CustomerDTO.class));
    }

    public Mono<ServerResponse> addCustomer(ServerRequest request) {
        return customerService.addCustomer(request.bodyToMono(CustomerDTO.class))
                .flatMap(savedCustomerDto -> {
                    return ServerResponse
                            .created(UriComponentsBuilder.fromPath(CustomerRouteConfig.CUSTOMER_ID)
                                    .build(savedCustomerDto.getId()))
                            .build();
                });
    }

}
