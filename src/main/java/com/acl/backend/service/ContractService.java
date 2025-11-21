package com.acl.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.acl.backend.data.AnalysisData;
import com.acl.backend.model.Contract;
import com.acl.backend.repository.ContractRepository;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final NLPAnalysisService nlpAnalysisService;

    public ContractService(
            ContractRepository contractRepository,
            NLPAnalysisService nlpAnalysisService) {
        this.contractRepository = contractRepository;
        this.nlpAnalysisService = nlpAnalysisService;
    }

    public Contract saveWithAnalysis(String name, String content, Long userId) {
        AnalysisData.AnalysisResult analysis = nlpAnalysisService.analyze(content);

        Contract c = new Contract();
        c.setName(name);
        c.setContent(content);
        c.setType(analysis.getType());
        c.setKeyClauses(analysis.getKeyClauses());
        c.setRisks(analysis.getRisks());
        c.setRiskScore(analysis.getRiskScore());
        c.setUserId(userId);

        Contract saved = contractRepository.save(c);
        return saved;
    }

    public Optional<Contract> findById(String id) {
        return contractRepository.findById(id);
    }

    public List<Contract> listAll() {
        return contractRepository.findAll();
    }

    public List<Contract> listByUser(Long userId) {
        return contractRepository.findByUserId(userId);
    }

    /**
     * Detecta cláusulas importantes que faltan en el contrato
     */
    private List<String> detectMissingClauses(AnalysisData.AnalysisResult analysis) {
        List<String> missing = new ArrayList<>();
        List<String> keyClauses = analysis.getKeyClauses();

        if (keyClauses == null || keyClauses.isEmpty()) {
            return missing;
        }

        // Convertir a minúsculas para búsqueda
        String clausesText = String.join(" ", keyClauses).toLowerCase();

        // Verificar cláusulas esenciales
        if (!clausesText.contains("terminación") && !clausesText.contains("terminacion")) {
            missing.add("Cláusula de Terminación");
        }

        if (!clausesText.contains("confidencialidad")) {
            missing.add("Cláusula de Confidencialidad");
        }

        if (!clausesText.contains("responsabilidad")) {
            missing.add("Limitación de Responsabilidad");
        }

        if (!clausesText.contains("jurisdicción") && !clausesText.contains("jurisdiccion")) {
            missing.add("Cláusula de Jurisdicción");
        }

        if (!clausesText.contains("fuerza mayor")) {
            missing.add("Cláusula de Fuerza Mayor");
        }

        return missing;
    }
}
