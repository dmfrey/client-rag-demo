package com.example.clientragdemo.documents.application.domain.model;

public enum ContentType {

    PDF, DOCX, TXT;

    public static ContentType fromFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename must not be blank");
        }

        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            throw new IllegalArgumentException("Unsupported file type: " + filename);
        }

        String extension = filename.substring(lastDot + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> PDF;
            case "docx" -> DOCX;
            case "txt" -> TXT;
            default -> throw new IllegalArgumentException("Unsupported file type: " + filename);
        };
    }
}
