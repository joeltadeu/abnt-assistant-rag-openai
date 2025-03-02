package com.ai.abnt.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class IngestionService {

  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
  private final VectorStore vectorStore;

  @Value("classpath:/docs/ABNT_NBR_6023_2018_Versao_Corrigida_2_20.pdf")
  private Resource pdfResource;

  public IngestionService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @PostConstruct
  public void init() {

    var config = PdfDocumentReaderConfig.builder()
            .withPageExtractedTextFormatter(
                    new ExtractedTextFormatter.Builder()
                            .build())
            .build();

    var pdfReader = new PagePdfDocumentReader(pdfResource, config);
    var textSplitter = new TokenTextSplitter();
    vectorStore.accept(textSplitter.apply(pdfReader.get()));
    log.info("VectorStore Loaded with data!");
  }
}
