package com.rcelik.springguru.reactivemongodb.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document
public class Beer {
    @Id
    private String id;
    private String beerName;
    private String beerStyle;
    private String upc;
    private Integer quantitiyOnHand;
    private BigDecimal price;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
