package com.rcelik.springguru.reactivemongodb.web.fn;

import org.hibernate.validator.internal.util.privilegedactions.GetAnnotationAttribute;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CustomerRouteConfig {

    public static final String CUSTOMER_PATH = "/api/v3/customers";
    public static final String CUSTOMER_ID = CUSTOMER_PATH + "/{id}";

    private final CustomerHandler handler;

    @Bean
    RouterFunction<ServerResponse> customerRouters() {
        return RouterFunctions.route()
                .GET(CUSTOMER_PATH, RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::getAllCustomers)
                .GET(CUSTOMER_ID, RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::getCustomerById)
                .POST(CUSTOMER_PATH, RequestPredicates.contentType(MediaType.APPLICATION_JSON), handler::addCustomer)
                .build();
    }

}
