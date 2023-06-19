package com.rcelik.springguru.reactivemongodb.web.fn;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;

import com.rcelik.springguru.reactivemongodb.model.CustomerDTO;
import com.rcelik.springguru.reactivemongodb.service.CustomerService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerHandler {

    private final CustomerService customerService;
    private final Validator validator;

    private void validateObject(CustomerDTO customerDto) {
        Errors errors = new BeanPropertyBindingResult(customerDto, "customerDtoModel");
        validator.validate(customerDto, errors);

        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }

    public Mono<ServerResponse> getAllCustomers(ServerRequest request) {
        return ServerResponse.ok().body(customerService.getAllCustomers(), CustomerDTO.class);
    }

    public Mono<ServerResponse> getCustomerById(ServerRequest request) {
        return customerService.getCustomerById(request.pathVariable("id"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(customerDto -> ServerResponse.ok().bodyValue(customerDto));
    }

    public Mono<ServerResponse> addCustomer(ServerRequest request) {
        return customerService.addCustomer(
                request.bodyToMono(CustomerDTO.class)
                        .doOnNext(this::validateObject))
                .flatMap(savedCustomerDto -> {
                    return ServerResponse
                            .created(UriComponentsBuilder.fromPath(CustomerRouteConfig.CUSTOMER_ID)
                                    .build(savedCustomerDto.getId()))
                            .build();
                });
    }

    public Mono<ServerResponse> updateCustomer(ServerRequest request) {
        return request.bodyToMono(CustomerDTO.class)
                .doOnNext(this::validateObject)
                .flatMap(requestedCustomer -> {
                    return customerService.updateCustomer(requestedCustomer, request.pathVariable("id"));
                }).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(updatedCustomer -> {
                    return ServerResponse.noContent().build();
                });
    }

    public Mono<ServerResponse> deleteCustomerById(ServerRequest request) {
        return customerService.deleteCustomer(request.pathVariable("id"))
                .then(ServerResponse.noContent().build());
    }

}
