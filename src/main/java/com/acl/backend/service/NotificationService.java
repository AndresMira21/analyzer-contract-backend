package com.acl.backend.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.acl.backend.model.Contract;
import com.acl.backend.model.Notification;
import com.acl.backend.model.User;
import com.acl.backend.repository.NotificationRepository;
import com.acl.backend.repository.UserRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final double HIGH_RISK_THRESHOLD = 50.0;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Notifica si el contrato tiene alto riesgo
     */
    public void notifyHighRisk(Contract contract, double riskScore) {
        if (riskScore < HIGH_RISK_THRESHOLD) {
            log.warn("Contrato de alto riesgo detectado: {} (Score: {})",
                    contract.getName(), riskScore);

            String message = String.format(
                    "ALERTA: El contrato '%s' tiene un nivel de riesgo alto (%.1f/100). " +
                            "Se recomienda revisión legal profesional.",
                    contract.getName(), riskScore
            );

            createNotification(
                    contract.getUserId(),
                    "Alto Riesgo Detectado",
                    message,
                    "high_risk",
                    contract.getId()
            );

        }
    }

    /**
     * Notifica sobre cláusulas importantes faltantes
     */
    public void notifyMissingClauses(Contract contract, List<String> missingClauses) {
        if (missingClauses != null && !missingClauses.isEmpty()) {
            log.info("Cláusulas faltantes en contrato {}: {}",
                    contract.getName(), missingClauses);

            String clausesList = String.join(", ", missingClauses);
            String message = String.format(
                    " El contrato '%s' no contiene las siguientes cláusulas importantes: %s. " +
                            "Considere agregar estas cláusulas para mayor protección.",
                    contract.getName(), clausesList
            );

            createNotification(
                    contract.getUserId(),
                    "Cláusulas Faltantes",
                    message,
                    "missing_clauses",
                    contract.getId()
            );
        }
    }

    /**
     * Notifica cuando se completa el análisis de un contrato
     */
    public void notifyAnalysisComplete(Contract contract) {
        log.info("Análisis completado para contrato: {}", contract.getName());

        String message = String.format(
                "El análisis del contrato '%s' ha sido completado exitosamente. " +
                        "Revisa los resultados en tu dashboard.",
                contract.getName()
        );

        createNotification(
                contract.getUserId(),
                "Análisis Completado",
                message,
                "analysis_complete",
                contract.getId()
        );
    }

    /**
     * Notifica sobre riesgos específicos encontrados
     */
    public void notifySpecificRisks(Contract contract, List<String> risks) {
        if (risks != null && risks.size() >= 3) { // 3 o más riesgos
            log.warn("Múltiples riesgos detectados en contrato: {}", contract.getName());

            String risksList = risks.stream()
                    .limit(3)
                    .map(r -> "• " + r)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            String message = String.format(
                    "Se detectaron múltiples riesgos en el contrato '%s':\n\n%s\n\n" +
                            "Se recomienda revisión detallada.",
                    contract.getName(), risksList
            );

            createNotification(
                    contract.getUserId(),
                    "Riesgos Detectados",
                    message,
                    "specific_risks",
                    contract.getId()
            );
        }
    }

    /**
     * Notifica sobre una comparación de contratos
     */
    public void notifyContractComparison(Long userId, String contract1Name, String contract2Name) {
        String message = String.format(
                "La comparación entre '%s' y '%s' está lista. " +
                        "Revisa las diferencias clave en tu dashboard.",
                contract1Name, contract2Name
        );

        createNotification(
                userId,
                "Comparación Lista",
                message,
                "comparison_ready",
                null
        );
    }

    /**
     * Obtiene todas las notificaciones de un usuario
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Obtiene notificaciones no leídas
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Marca una notificación como leída
     */
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        });
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    public void markAllAsRead(Long userId) {
        List<Notification> unread = getUnreadNotifications(userId);
        unread.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        });
        notificationRepository.saveAll(unread);
    }

    /**
     * Elimina una notificación
     */
    public void deleteNotification(String notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUserId().equals(userId)) {
                notificationRepository.delete(notification);
            }
        });
    }

    /**
     * Elimina todas las notificaciones leídas de un usuario
     */
    public void deleteReadNotifications(Long userId) {
        List<Notification> read = notificationRepository
                .findByUserIdAndReadTrueOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(read);
    }

    // Metodos privados

    private void createNotification(Long userId, String title, String message, String notificationType, String contractId) {
        try {
            if (userId == null) {
                log.warn("No se puede crear notificación: userId es null");
                return;
            }

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(notificationType);
            notification.setContractId(contractId);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            if (notificationRepository != null) {
                notificationRepository.save(notification);
                log.info("Notificación creada para usuario {}: {}", userId, title);
            } else {
                log.error("NotificationRepository no está inicializado");
            }
        } catch (Exception e) {
            log.error("Error al crear notificación: {}", e.getMessage());
        }
    }
}
