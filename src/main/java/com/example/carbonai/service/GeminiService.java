package com.example.carbonai.service;

import com.example.carbonai.dtos.MyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/*
This code utilizes WebClient along with several other classes from org.springframework.web.reactive.
However, the code is NOT reactive due to the use of the block() method, which bridges the reactive code (WebClient)
to our imperative code (the way we have used Spring Boot up until now).

You will not truly benefit from WebClient unless you need to make several external requests in parallel.
Additionally, the WebClient API is very clean, so if you are familiar with HTTP, it should be easy to
understand what's going on in this code.
*/

@Service
public class GeminiService {

  public static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

  // TODO:
  /*
  @Value("${app.gemini-api-key}")
  private String API_KEY;

  @Value("${app.url}")
  public String URL;

  @Value("${app.model}")
  public String MODEL;

  @Value("${app.temperature}")
  public double TEMPERATURE;

  @Value("${app.max_tokens}")
  public int MAX_TOKENS;

  @Value("${app.frequency_penalty}")
  public double FREQUENCY_PENALTY;

  @Value("${app.presence_penalty}")
  public double PRESENCE_PENALTY;

  @Value("${app.top_p}")
  public double TOP_P;
*/
  private WebClient client;

  public GeminiService() {
    this.client = WebClient.create();
  }

  // Use this constructor for testing, to inject a mock client
  public GeminiService(WebClient client) {
    this.client = client;
  }
}