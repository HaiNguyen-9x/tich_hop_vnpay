package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Sort {
    private Integer pageNo;
    private Integer pageSize;
    private String sortBy;
    private String sortDir;
    private String search;
    private Integer totalPages;
}
