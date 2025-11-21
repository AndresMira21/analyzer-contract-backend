package com.acl.backend.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acl.backend.model.Contract;
import com.acl.backend.model.User;
import com.acl.backend.repository.ChatRepository;
import com.acl.backend.repository.UserRepository;
import com.acl.backend.service.ContractService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ContractService contractService;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final com.acl.backend.repository.DeletedContractRepository deletedContractRepository;

    public DashboardController(
            ContractService contractService,
            UserRepository userRepository,
            ChatRepository chatRepository,
            com.acl.backend.repository.DeletedContractRepository deletedContractRepository) {
        this.contractService = contractService;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.deletedContractRepository = deletedContractRepository;
    }

    /**
     * Obtiene estadísticas completas del dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Contract> contracts = contractService.listByUser(user.getId());

        DashboardStats stats = new DashboardStats();

        // Estadísticas básicas
        long deletedCount = deletedContractRepository.countByUserId(user.getId());
        stats.setTotalContracts(contracts.size());
        stats.setDeletedContracts(deletedCount);
        stats.setHighRiskContracts(
                contracts.stream()
                        .filter(c -> c.getRiskScore() != null && c.getRiskScore() < 50)
                        .count()
        );
        stats.setMediumRiskContracts(
                contracts.stream()
                        .filter(c -> c.getRiskScore() != null &&
                                                c.getRiskScore() >= 50 && c.getRiskScore() < 75)
                        .count()
        );
        stats.setLowRiskContracts(
                contracts.stream()
                        .filter(c -> c.getRiskScore() != null && c.getRiskScore() >= 75)
                        .count()
        );

        // Contratos por tipo
        Map<String, Long> contractsByType = contracts.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getType() != null ? c.getType() : "Sin clasificar",
                        Collectors.counting()
                ));
        stats.setContractsByType(contractsByType);

        // Promedio de riesgo
        double avgRisk = contracts.stream()
                .filter(c -> c.getRiskScore() != null)
                .mapToDouble(Contract::getRiskScore)
                .average()
                .orElse(0.0);
        stats.setAverageRiskScore(avgRisk);

        // Contratos recientes (últimos 7 días)
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long recentContracts = contracts.stream()
                .filter(c -> c.getUploadedAt().isAfter(sevenDaysAgo))
                .count();
        stats.setRecentContracts(recentContracts);

        // Estadísticas de chat
        long totalChatMessages = contracts.stream()
                .mapToLong(c -> chatRepository.countByContractId(c.getId()))
                .sum();
        stats.setTotalChatMessages(totalChatMessages);


        // Top 5 contratos con más riesgo
        List<ContractSummary> highestRiskContracts = contracts.stream()
                .filter(c -> c.getRiskScore() != null)
                .sorted((c1, c2) -> Double.compare(c1.getRiskScore(), c2.getRiskScore()))
                .limit(5)
                .map(this::toContractSummary)
                .collect(Collectors.toList());
        stats.setHighestRiskContracts(highestRiskContracts);

        // Cláusulas más comunes encontradas
        Map<String, Long> commonClauses = contracts.stream()
                .filter(c -> c.getKeyClauses() != null)
                .flatMap(c -> c.getKeyClauses().stream())
                .collect(Collectors.groupingBy(
                        clause -> clause,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
        stats.setCommonClauses(commonClauses);

        // Riesgos más frecuentes
        Map<String, Long> commonRisks = contracts.stream()
                .filter(c -> c.getRisks() != null)
                .flatMap(c -> c.getRisks().stream())
                .collect(Collectors.groupingBy(
                        risk -> risk,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
        stats.setCommonRisks(commonRisks);

        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene actividad reciente del usuario
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityItem>> getRecentActivity(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Contract> recentContracts = contractService.listByUser(user.getId()).stream()
                .sorted((c1, c2) -> c2.getUploadedAt().compareTo(c1.getUploadedAt()))
                .limit(10)
                .collect(Collectors.toList());

        List<ActivityItem> activity = recentContracts.stream()
                .map(contract -> {
                    ActivityItem item = new ActivityItem();
                    item.setType("contract_upload");
                    item.setTitle("Contrato analizado: " + contract.getName());
                    item.setDescription(String.format(
                            "Tipo: %s, Riesgo: %.1f/100",
                            contract.getType(),
                            contract.getRiskScore()
                    ));
                    item.setTimestamp(contract.getUploadedAt());
                    item.setContractId(contract.getId());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(activity);
    }

    // Metodo auxiliar

    private ContractSummary toContractSummary(Contract contract) {
        ContractSummary summary = new ContractSummary();
        summary.setId(contract.getId());
        summary.setName(contract.getName());
        summary.setType(contract.getType());
        summary.setRiskScore(contract.getRiskScore());
        summary.setUploadedAt(contract.getUploadedAt());
        return summary;
    }

    // Data

    public static class DashboardStats {
        private int totalContracts;
        private long highRiskContracts;
        private long mediumRiskContracts;
        private long lowRiskContracts;
        private Map<String, Long> contractsByType;
        private double averageRiskScore;
        private long recentContracts;
        private long totalChatMessages;
        private long deletedContracts;
        private List<ContractSummary> highestRiskContracts;
        private Map<String, Long> commonClauses;
        private Map<String, Long> commonRisks;

        public int getTotalContracts() { return totalContracts; }
        public void setTotalContracts(int totalContracts) { this.totalContracts = totalContracts; }

        public long getHighRiskContracts() { return highRiskContracts; }
        public void setHighRiskContracts(long highRiskContracts) { this.highRiskContracts = highRiskContracts; }

        public long getMediumRiskContracts() { return mediumRiskContracts; }
        public void setMediumRiskContracts(long mediumRiskContracts) { this.mediumRiskContracts = mediumRiskContracts; }

        public long getLowRiskContracts() { return lowRiskContracts; }
        public void setLowRiskContracts(long lowRiskContracts) { this.lowRiskContracts = lowRiskContracts; }

        public Map<String, Long> getContractsByType() { return contractsByType; }
        public void setContractsByType(Map<String, Long> contractsByType) { this.contractsByType = contractsByType; }

        public double getAverageRiskScore() { return averageRiskScore; }
        public void setAverageRiskScore(double averageRiskScore) { this.averageRiskScore = averageRiskScore; }

        public long getRecentContracts() { return recentContracts; }
        public void setRecentContracts(long recentContracts) { this.recentContracts = recentContracts; }

        public long getTotalChatMessages() { return totalChatMessages; }
        public void setTotalChatMessages(long totalChatMessages) { this.totalChatMessages = totalChatMessages; }

        public long getDeletedContracts() { return deletedContracts; }
        public void setDeletedContracts(long deletedContracts) { this.deletedContracts = deletedContracts; }

        public List<ContractSummary> getHighestRiskContracts() { return highestRiskContracts; }
        public void setHighestRiskContracts(List<ContractSummary> highestRiskContracts) { this.highestRiskContracts = highestRiskContracts; }

        public Map<String, Long> getCommonClauses() { return commonClauses; }
        public void setCommonClauses(Map<String, Long> commonClauses) { this.commonClauses = commonClauses; }

        public Map<String, Long> getCommonRisks() { return commonRisks; }
        public void setCommonRisks(Map<String, Long> commonRisks) { this.commonRisks = commonRisks; }
    }

    public static class ContractSummary {
        private String id;
        private String name;
        private String type;
        private Double riskScore;
        private Instant uploadedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Double getRiskScore() { return riskScore; }
        public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

        public Instant getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    }

    public static class ActivityItem {
        private String type;
        private String title;
        private String description;
        private Instant timestamp;
        private String contractId;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public String getContractId() { return contractId; }
        public void setContractId(String contractId) { this.contractId = contractId; }
    }
}
