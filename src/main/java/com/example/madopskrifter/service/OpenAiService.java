package com.example.madopskrifter.service;

import com.example.madopskrifter.dtos.ChatCompletionRequest;
import com.example.madopskrifter.dtos.ChatCompletionResponse;
import com.example.madopskrifter.dtos.MyResponse;
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

@Service
public class OpenAiService {

  private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

  @Value("${app.api-key}")
  private String API_KEY;

  @Value("${app.url}")
  private String URL;

  @Value("${app.model}")
  private String MODEL;

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

  private final WebClient client;

  public OpenAiService(WebClient.Builder webClientBuilder) {
    this.client = webClientBuilder.build();
  }

  public MyResponse makeRequest(String userPrompt) {
    // Opdater systemmeddelelsen, så OpenAI får instruktioner i at bruge Salling Group API
    String systemMessage = "Du er en hjælpsom kok-assistent. Når du modtager en forespørgsel om en opskrift, " +
            "skal du søge efter ingredienser på https://api.sallinggroup.com/v1-beta/product-suggestions/relevant-products " +
            "baseret på brugerens forespørgsel. Foreslå en opskrift ved at bruge ingredienserne fra Salling Group API." +
            "API'et kan tilgås ved at lave en GET-forespørgsel med parameteren 'query', som indeholder navnet på maden eller ingrediensen."+"Sæt en total sum af prisen for retten, for alle varende i bunden";


    ChatCompletionRequest requestDto = new ChatCompletionRequest();
    requestDto.setModel(MODEL);
    requestDto.setTemperature(TEMPERATURE);
    requestDto.setMax_tokens(MAX_TOKENS);
    requestDto.setTop_p(TOP_P);
    requestDto.setFrequency_penalty(FREQUENCY_PENALTY);
    requestDto.setPresence_penalty(PRESENCE_PENALTY);
    requestDto.getMessages().add(new ChatCompletionRequest.Message("system", systemMessage));
    requestDto.getMessages().add(new ChatCompletionRequest.Message("user", userPrompt));

    ObjectMapper mapper = new ObjectMapper();
    String json = "";
    String err =  null;
    try {
      json = mapper.writeValueAsString(requestDto);
      System.out.println(json);
      ChatCompletionResponse response = client.post()
              .uri(new URI(URL))
              .header("Authorization", "Bearer " + API_KEY)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(requestDto))
              .retrieve()
              .bodyToMono(ChatCompletionResponse.class)
              .block();

      String responseMsg = response.getChoices().get(0).getMessage().getContent();
      return new MyResponse(responseMsg);
    } catch (WebClientResponseException e) {
      logger.error("Fejl fra OpenAI API: " + e.getResponseBodyAsString());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fejl ved generering af svar.");
    } catch (Exception e) {
      logger.error("Uventet fejl: ", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Uventet fejl opstod.");
    }
  }
}
