package com.rcelik.springguru.reactivemongodb.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@Document
public class Customer {
    @Id
    private String id;
    private String name;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
