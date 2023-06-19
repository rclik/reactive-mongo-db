package com.rcelik.springguru.reactivemongodb.model;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank
    @Size(min = 3, max = 20)
    private String name;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
