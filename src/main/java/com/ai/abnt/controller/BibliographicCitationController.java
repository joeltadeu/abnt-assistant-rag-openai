package com.ai.abnt.controller;

import com.ai.abnt.model.BibliographicCitation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BibliographicCitationController {
  private final ChatClient chatClient;

  private final String PROMPT_EN_US =
      "Create a correct bibliographic reference, following ABNT style guidelines, for the %s detailed below: %s";

  private final String PROMPT_PT_BR =
          "Corrigir a referência bibliográfica, utilizando o manual de formatação ABNT para o %s detalhada a seguir: %s";

  private final VectorStore vectorStore;

  public BibliographicCitationController(ChatClient.Builder builder, VectorStore vectorStore) {
    this.chatClient = builder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore)).build();
    this.vectorStore = vectorStore;
  }

  @PostMapping("/v1/references/abnt")
  public String chat(@RequestBody BibliographicCitation prompt) {
    return chatClient
        .prompt()
        .user(PROMPT_PT_BR.formatted(prompt.type().getDescription(), prompt.reference()))
        .call()
        .content();
  }

  @PostMapping("/v2/references/abnt")
  public ResponseEntity<String> generateAnswer(@RequestBody BibliographicCitation request) {
    List<Document> similarDocuments = vectorStore.similaritySearch(request.reference());
    String information = similarDocuments.stream()
            .map(Document::getText)
            .collect(Collectors.joining(System.lineSeparator()));
    var systemPromptTemplate = new SystemPromptTemplate(
            """
                        You are a helpful assistant.
                        Use only the following information to answer the question.
                        Do not use any other information. If you do not know, simply answer: Unknown.

                        {information}
                    """);
    var systemMessage = systemPromptTemplate.createMessage(Map.of("information", information));
    var userPromptTemplate = new PromptTemplate("{query}");
    var userMessage = userPromptTemplate.createMessage(Map.of("query", PROMPT_PT_BR.formatted(request.type().getDescription(), request.reference())));
    var prompt = new Prompt(List.of(systemMessage, userMessage));
    return ResponseEntity.ok(chatClient.prompt(prompt).call().content());
  }
}
