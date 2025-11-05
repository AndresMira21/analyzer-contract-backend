package com.acl.backend.service;

import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractionService {

    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "file";
        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return extractFromDocx(file.getInputStream());
        } else if (filename.endsWith(".doc")) {
            return extractFromDoc(file.getInputStream());
        }else {
            throw new IllegalArgumentException("Formato no soportado. Use PDF o DOCX.");
        }
    }

    private String extractFromPdf(InputStream in) throws Exception {
        try (PDDocument doc = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractFromDocx(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
            return ex.getText();
        }
    }

    private String extractFromDoc(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
            return ex.getText();
        }
    }
}
