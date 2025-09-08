package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.NotificationTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {

    private Long id;

    private String templateName;

    private NotificationTemplate.TemplateType templateType;

    private String subject;

    private String content;

    private List<String> variables;

    private Boolean isActive;

    private String language;

    private String category;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Usage statistics
    private Long usageCount;

    private LocalDateTime lastUsedAt;

    private Double successRate;

    // Template metadata
    private String version;

    private String author;

    private List<String> tags;

    // Template settings
    private NotificationTemplateRequest.TemplateSettings settings;

    // Validation status
    private Boolean isValid;

    private List<String> validationErrors;

    // Preview information
    private String previewHtml;

    private Long estimatedSize;

    // Analytics
    private TemplateAnalytics analytics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateAnalytics {
        private Long totalSent;

        private Long totalDelivered;

        private Long totalOpened;

        private Long totalClicked;

        private Long totalBounced;

        private Long totalFailed;

        private Double deliveryRate;

        private Double openRate;

        private Double clickRate;

        private Double bounceRate;

        private Double averageDeliveryTime;

        private LocalDateTime lastAnalyticsUpdate;
    }

    // Static factory methods
    public static NotificationTemplateResponse fromEntity(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .templateType(template.getTemplateType())
                .subject(template.getSubject())
                .content(template.getContent())
                .variables(parseVariables(template.getVariables()))
                .isActive(template.getIsActive())
                .language(template.getLanguage())
                .category(template.getCategory())
                .description(template.getDescription())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private static List<String> parseVariables(String variablesJson) {
        // Parse JSON string to list of variables
        // Implementation would use Jackson or similar
        return List.of(); // Placeholder
    }
}
