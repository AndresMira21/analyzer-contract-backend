package com.acl.backend.service;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.acl.backend.data.AnalysisData;
import com.acl.backend.model.Contract;

@Service
public class ReportService {

    private static final String DISCLAIMER = "Este sistema no sustituye asesoría jurídica profesional.";

    public byte[] generatePdf(Contract contract, AnalysisData.AnalysisResult analysis) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - 50;

                y = writeTitle(cs, "Reporte de Análisis de Contrato", 50, y);
                y = writeText(cs, "Nombre: " + contract.getName(), 12, 50, y);
                y = writeText(cs, "Tipo estimado: " + analysis.getType(), 12, 50, y);
                y = writeText(cs, "Subido: " + contract.getUploadedAt(), 12, 50, y);

                y = writeSubTitle(cs, "Cláusulas clave detectadas:", 50, y - 10);
                for (String c : analysis.getKeyClauses()) {
                    y = writeText(cs, "- " + c, 11, 60, y);
                }

                y = writeSubTitle(cs, "Riesgos:", 50, y - 10);
                for (String r : analysis.getRisks()) {
                    y = writeText(cs, "- " + r, 11, 60, y);
                }

                y = writeText(cs, "Puntaje de riesgo: " + String.format("%.1f", analysis.getRiskScore()), 12, 50, y - 5);

                y = writeSubTitle(cs, "Recomendaciones:", 50, y - 10);
                for (String rec : analysis.getRecommendations()) {
                    y = writeText(cs, "- " + rec, 11, 60, y);
                }

                y = writeSubTitle(cs, "Resumen:", 50, y - 10);
                y = writeParagraph(cs, analysis.getSummary(), 11, 60, y, page.getMediaBox().getWidth() - 100);

                y = writeSubTitle(cs, "Disclaimer:", 50, y - 10);
                y = writeParagraph(cs, DISCLAIMER, 10, 60, y, page.getMediaBox().getWidth() - 100);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        }
    }

    private float writeTitle(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 24;
    }

    private float writeSubTitle(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 18;
    }

    private float writeText(PDPageContentStream cs, String text, int size, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, size);
        cs.newLineAtOffset(x, y);
        cs.showText(cut(text, 100));
        cs.endText();
        return y - (size + 6);
    }

    private float writeParagraph(PDPageContentStream cs, String text, int size, float x, float y, float width) throws Exception {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if ((line + w).length() > 110) {
                y = writeText(cs, line.toString(), size, x, y);
                line = new StringBuilder();
            }
            line.append(w).append(" ");
        }
        if (!line.isEmpty()) {
            y = writeText(cs, line.toString(), size, x, y);
        }
        return y;
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}