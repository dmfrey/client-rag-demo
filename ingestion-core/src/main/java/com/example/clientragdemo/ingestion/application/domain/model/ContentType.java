package com.example.clientragdemo.ingestion.application.domain.model;

public enum ContentType {

    PDF, DOC, DOCX, TXT, PNG, JPEG, GIF;

    public boolean isImage() {
        return this == PNG || this == JPEG || this == GIF;
    }

    public String mimeType() {
        return switch (this) {
            case PNG -> "image/png";
            case JPEG -> "image/jpeg";
            case GIF -> "image/gif";
            case PDF -> "application/pdf";
            case DOC -> "application/msword";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case TXT -> "text/plain";
        };
    }

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
            case "doc" -> DOC;
            case "docx" -> DOCX;
            case "txt" -> TXT;
            case "png" -> PNG;
            case "jpg", "jpeg" -> JPEG;
            case "gif" -> GIF;
            default -> throw new IllegalArgumentException("Unsupported file type: " + filename);
        };
    }
}
