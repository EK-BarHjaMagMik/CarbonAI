package com.example.madopskrifter.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClearanceDTO {
    private String currency;
    private double discount;
    private String ean;
    private String endTime;
    private String lastUpdate;
    private double newPrice;
    private double originalPrice;
    private double percentDiscount;
    private String startTime;
    private double stock;
    private String stockUnit;
}
