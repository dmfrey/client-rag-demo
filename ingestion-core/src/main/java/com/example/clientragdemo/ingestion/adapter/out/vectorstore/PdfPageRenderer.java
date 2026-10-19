package com.example.clientragdemo.ingestion.adapter.out.vectorstore;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Renders PDF pages to PNG so a vision model can read a scanned (image-only) PDF. */
final class PdfPageRenderer {

    private static final float DPI = 150f;

    private PdfPageRenderer() {
    }

    static int pageCount(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<byte[]> render(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<byte[]> pages = new ArrayList<>();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, DPI, ImageType.RGB);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(image, "png", png);
                pages.add(png.toByteArray());
            }
            return pages;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
