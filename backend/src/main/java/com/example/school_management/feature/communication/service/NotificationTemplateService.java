package com.example.school_management.feature.communication.service;

import com.example.school_management.feature.communication.dto.NotificationTemplateRequest;
import com.example.school_management.feature.communication.dto.NotificationTemplateResponse;
import com.example.school_management.feature.communication.entity.NotificationTemplate;

import java.util.List;
import java.util.Map;

public interface NotificationTemplateService {

    /**
     * Create a new notification template
     */
    NotificationTemplateResponse createTemplate(NotificationTemplateRequest request);

    /**
     * Update an existing template
     */
    NotificationTemplateResponse updateTemplate(Long templateId, NotificationTemplateRequest request);

    /**
     * Get template by ID
     */
    NotificationTemplateResponse getTemplate(Long templateId);

    /**
     * Get template by name and type
     */
    NotificationTemplateResponse getTemplate(String templateName, NotificationTemplate.TemplateType templateType, String language);

    /**
     * Get all templates
     */
    List<NotificationTemplateResponse> getAllTemplates();

    /**
     * Get templates by type
     */
    List<NotificationTemplateResponse> getTemplatesByType(NotificationTemplate.TemplateType templateType);

    /**
     * Get active templates
     */
    List<NotificationTemplateResponse> getActiveTemplates();

    /**
     * Search templates
     */
    List<NotificationTemplateResponse> searchTemplates(String searchTerm);

    /**
     * Delete template
     */
    void deleteTemplate(Long templateId);

    /**
     * Activate/Deactivate template
     */
    NotificationTemplateResponse toggleTemplateStatus(Long templateId, boolean isActive);

    /**
     * Process template with variables
     */
    String processTemplate(String templateContent, Map<String, Object> variables);

    /**
     * Validate template syntax
     */
    boolean validateTemplate(String templateContent);

    /**
     * Get template variables
     */
    List<String> extractTemplateVariables(String templateContent);

    /**
     * Clone template
     */
    NotificationTemplateResponse cloneTemplate(Long templateId, String newTemplateName);

    /**
     * Get template usage statistics
     */
    Map<String, Object> getTemplateUsageStats(Long templateId);

    /**
     * Initialize default templates
     */
    void initializeDefaultTemplates();
}
