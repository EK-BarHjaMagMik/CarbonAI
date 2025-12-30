package com.example.madopskrifter.api;

import com.example.madopskrifter.dtos.MyResponse;
import com.example.madopskrifter.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecipeController {

  private final OpenAiService openAiService;

  // Endpoint til at generere opskrifter via OpenAI
  @GetMapping("/generate")
  public ResponseEntity<MyResponse> genererOpskrift(@RequestParam("query") String query) {
    try {
      // Send forespørgsel til OpenAI for at generere opskrift
      MyResponse response = openAiService.makeRequest(query);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(new MyResponse("Intern serverfejl opstod under generering af opskriften."));
    }
  }
}
