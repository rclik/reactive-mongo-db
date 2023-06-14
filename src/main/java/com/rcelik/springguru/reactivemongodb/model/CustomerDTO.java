package com.rcelik.springguru.reactivemongodb.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerDTO {
    private String id;
    private String name;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
