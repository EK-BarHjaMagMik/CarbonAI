package com.example.madopskrifter.service;

import com.example.madopskrifter.dtos.ClearanceDTO;
import com.example.madopskrifter.dtos.Product;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;

@Service
public class SallingGroupService {

    private static final Logger logger = LoggerFactory.getLogger(SallingGroupService.class);

    @Value("${app.salling-api-key}")
    private String sallingApiKey;

    @Value("${app.salling-url}")
    private String sallingUrl;

    private final WebClient client;


    public SallingGroupService(WebClient.Builder webClientBuilder) {
        this.client = webClientBuilder.build();
    }

    public List<Product> fetchAllProducts() {
        String url = sallingUrl ; // Antag, at dette er det korrekte endpoint for at hente alle varer

        try {
            return client.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + sallingApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(Product.class) // Hent listen af produkter
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("Salling Group API error: " + e.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getRawStatusCode()),
                    "Fejl ved hentning af produktinformation fra Salling Group API"
            );
        }
    }

    public List<ClearanceDTO> fetchAllFoodWaste() {
        String url = "https://api.sallinggroup.com/v1/food-waste/efba0457-090e-4132-81ba-c72b4c8e7fee";

        try {
            return client.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + sallingApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(ClearanceDTO.class) // Konverterer respons til en liste af ClearanceDTO objekter
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            logger.error("Salling Group API error: " + e.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getRawStatusCode()),
                    "Fejl ved hentning af produktinformation fra Salling Group API"
            );
        } catch (Exception e) {
            logger.error("Uventet fejl: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Intern serverfejl");
        }
    }

        public List<Product> fetchRelevantProductInfo (String query) {
            String salling = "https://api.sallinggroup.com/v1-beta/product-suggestions/relevant-products?query=";
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(salling)
                    .queryParam("query", query);

            String url = uriBuilder.toUriString();

            try {
                return client.get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sallingApiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToFlux(Product.class) // Hent listen af produkter
                        .collectList()
                        .block();
            } catch (WebClientResponseException e) {
                logger.error("Salling Group API error: " + e.getResponseBodyAsString());
                throw new ResponseStatusException(
                        HttpStatus.valueOf(e.getRawStatusCode()),
                        "Fejl ved hentning af relevant produktinformation fra Salling Group API"
                );
            } catch (Exception e) {
                logger.error("Uventet fejl: ", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Uventet fejl opstod");
            }
        }

}
