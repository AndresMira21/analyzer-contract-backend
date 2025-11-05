package com.acl.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.acl.backend.data.AnalysisData.AnalysisResult;

@Service
public class LegacyAnalysisService {

    private static final Map<String, Pattern> CLAUSE_PATTERNS = Map.of(
            "Confidencialidad", Pattern.compile("\\bconfidencial(?:idad|e)\\b|nda|no divulgaci[oó]n", Pattern.CASE_INSENSITIVE),
            "Vigencia", Pattern.compile("\\bvigencia\\b|\\bvencimiento\\b|\\bplazo\\b", Pattern.CASE_INSENSITIVE),
            "Sanciones", Pattern.compile("\\bpenalid(?:ad|ades)\\b|\\bmulta\\b|\\bsanci[oó]n", Pattern.CASE_INSENSITIVE),
            "Obligaciones", Pattern.compile("\\bobligaci[oó]n(?:es)?\\b|\\bdeber(?:es)?\\b", Pattern.CASE_INSENSITIVE),
            "Jurisdicción", Pattern.compile("\\bjurisdicci[oó]n\\b|\\bley aplicable\\b|\\bcompetencia\\b", Pattern.CASE_INSENSITIVE),
            "Terminación", Pattern.compile("\\bterminaci[oó]n\\b|\\brescisi[oó]n\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Map<String, Pattern> TYPE_PATTERNS = Map.of(
            "Arrendamiento", Pattern.compile("\\barriendo|arrendamiento|inquilino|renta\\b", Pattern.CASE_INSENSITIVE),
            "Laboral", Pattern.compile("\\bempleado|empleador|salario|n[oó]mina|jornada\\b", Pattern.CASE_INSENSITIVE),
            "Servicios", Pattern.compile("\\bservicio(?:s)?\\b|\\bprestaci[oó]n de servicios\\b", Pattern.CASE_INSENSITIVE),
            "NDA", Pattern.compile("\\bno divulgaci[oó]n\\b|\\bnda\\b|\\bconfidencialidad\\b", Pattern.CASE_INSENSITIVE),
            "Compraventa", Pattern.compile("\\bcompra(?:venta)?\\b|\\bprecio\\b|\\bentrega\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> RISK_PATTERNS = List.of(
            Pattern.compile("\\breserva(?:mos)? el derecho a modificar\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brenuncia(?:s)? a demandar\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bresponsabilidad ilimitada\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpenalidad excesiva\\b|\\bmulta.*(excesiva|desproporcionada)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpr[oó]rroga autom[aá]tica\\b", Pattern.CASE_INSENSITIVE)
    );

    public AnalysisResult analyzeWithRegex(String text) {
        String norm = normalize(text);

        // Detecta las clausulas
        List<String> clauses = CLAUSE_PATTERNS.entrySet().stream()
                .filter(e -> e.getValue().matcher(norm).find())
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        // Clasificacion
        String type = detectTypeWithRegex(norm);

        // Riesgos
        List<String> risks = new ArrayList<>();
        for (Pattern p : RISK_PATTERNS) {
            var m = p.matcher(norm);
            if (m.find()) risks.add(describeRisk(p));
        }
        if (!norm.toLowerCase().contains("limitación de responsabilidad")
                && !norm.toLowerCase().contains("limitacion de responsabilidad")) {
            risks.add("No se encontró cláusula de limitación de responsabilidad.");
        }
        if (!norm.toLowerCase().contains("terminación")
                && !norm.toLowerCase().contains("terminacion")) {
            risks.add("No se encontró cláusula de terminación.");
        }

        // Puntuacion
        double base = 100.0;
        base -= risks.size() * 10.0;
        base += Math.min(clauses.size(), 8) * 2.0;
        base = Math.max(0, Math.min(100, base));

        // Recomendaciones
        List<String> recs = new ArrayList<>();
        if (risks.stream().anyMatch(r -> r.toLowerCase().contains("limitación"))) {
            recs.add("Agregar cláusula de limitación de responsabilidad clara y razonable.");
        }
        if (risks.stream().anyMatch(r -> r.toLowerCase().contains("terminación"))) {
            recs.add("Especificar causales y procedimiento de terminación anticipada.");
        }
        if (type.equals("Servicios")) {
            recs.add("Definir entregables, SLA y métricas de calidad del servicio.");
        }
        recs.add("Verificar coherencia entre plazos, pagos y penalidades.");
        recs.add("Solicitar revisión profesional si el contrato es crítico.");

        AnalysisResult res = new AnalysisResult();
        res.setType(type);
        res.setKeyClauses(clauses);
        res.setRisks(risks);
        res.setRiskScore(base);
        res.setRecommendations(recs);
        res.setSummary("Análisis automático por reglas: se detectaron " + clauses.size()
                + " cláusulas clave, tipo estimado " + type + " y " + risks.size() + " riesgos potenciales.");
        return res;
    }

    public List<String> answerQuestionWithRegex(String text, String question) {
        String norm = normalize(text);
        String q = question.toLowerCase(Locale.ROOT);
        List<String> answers = new ArrayList<>();
        if (q.contains("plazo") || q.contains("vigencia")) {
            answers.add(extractSnippet(norm, "vigencia|plazo|vencimiento"));
        } else if (q.contains("pago") || q.contains("precio")) {
            answers.add(extractSnippet(norm, "pago|precio|facturaci[oó]n|honorarios"));
        } else if (q.contains("confidencial")) {
            answers.add(extractSnippet(norm, "confidencial"));
        } else if (q.contains("terminaci")) {
            answers.add(extractSnippet(norm, "terminaci[oó]n|rescisi[oó]n"));
        } else {
            answers.add("No se encontró información específica. Revise las cláusulas generales.");
        }
        return answers;
    }

    public String detectTypeWithRegex(String text) {
        return TYPE_PATTERNS.entrySet().stream()
                .filter(e -> e.getValue().matcher(text).find())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("General");
    }

    private String normalize(String t) {
        return t == null ? "" : t.replaceAll("\\s+", " ").trim();
    }

    private String extractSnippet(String text, String regex) {
        Pattern p = Pattern.compile("([^.\\n]{0,120}(" + regex + ")[^.\\n]{0,120})", Pattern.CASE_INSENSITIVE);
        var m = p.matcher(text);
        if (m.find()) return m.group(1) + "...";
        return "No se hallaron coincidencias relevantes.";
    }

    private String describeRisk(Pattern p) {
        String s = p.pattern();
        if (s.contains("modificar")) return "Cláusula que permite modificaciones unilaterales.";
        if (s.contains("renuncia")) return "Renuncia a acciones legales por parte del usuario.";
        if (s.contains("ilimitada")) return "Responsabilidad potencialmente ilimitada.";
        if (s.contains("excesiva")) return "Penalidad desproporcionada.";
        if (s.contains("pr[oó]rroga")) return "Prórroga automática sin aviso expreso.";
        return "Riesgo potencial detectado.";
    }
}
