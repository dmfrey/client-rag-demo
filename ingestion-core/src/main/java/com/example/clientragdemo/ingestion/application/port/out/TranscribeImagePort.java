package com.example.clientragdemo.ingestion.application.port.out;

/**
 * Reads the text out of an image (a photo/scan, or one rendered page of a scanned PDF). Only
 * available when a vision-capable GenAI service is bound - see GenaiVisionConfiguration - so
 * callers must treat its absence as "this deployment cannot read images", not as an error.
 */
public interface TranscribeImagePort {

    /** @return the text found in the image; blank if there is none */
    String transcribe(byte[] image, String mimeType);
}
