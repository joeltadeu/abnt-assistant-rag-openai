package com.ai.abnt.model;

public enum DocumentType {
    book("livro"), article("artigo científico");

    private String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
