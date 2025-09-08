package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.NotificationTemplateRequest;
import com.example.school_management.feature.communication.dto.NotificationTemplateResponse;
import com.example.school_management.feature.communication.entity.NotificationTemplate;
import com.example.school_management.feature.communication.repository.NotificationTemplateRepository;
import com.example.school_management.feature.communication.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    @Override
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setLanguage(request.getLanguage());
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        template.setDescription(request.getDescription());

        NotificationTemplate saved = templateRepository.save(template);
        log.info("Created notification template: {}", saved.getTemplateName());
        return mapToResponse(saved);
    }

    @Override
    public NotificationTemplateResponse updateTemplate(Long templateId, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setLanguage(request.getLanguage());
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : template.getIsActive());
        template.setDescription(request.getDescription());

        NotificationTemplate saved = templateRepository.save(template);
        log.info("Updated notification template: {}", saved.getTemplateName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplate(Long templateId) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));
        return mapToResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplate(String templateName, NotificationTemplate.TemplateType templateType, String language) {
        // For now, just find by template name and type - implement proper query later
        List<NotificationTemplate> templates = templateRepository.findAll().stream()
                .filter(t -> t.getTemplateName().equals(templateName) && 
                           t.getTemplateType().equals(templateType) && 
                           t.getLanguage().equals(language) && 
                           t.getIsActive())
                .collect(Collectors.toList());
        
        if (templates.isEmpty()) {
            throw new RuntimeException("Template not found: " + templateName);
        }
        
        return mapToResponse(templates.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getTemplatesByType(NotificationTemplate.TemplateType templateType) {
        return templateRepository.findAll().stream()
                .filter(t -> t.getTemplateType().equals(templateType))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getActiveTemplates() {
        return templateRepository.findAll().stream()
                .filter(NotificationTemplate::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> searchTemplates(String searchTerm) {
        return templateRepository.findAll().stream()
                .filter(t -> t.getTemplateName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                           (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchTerm.toLowerCase())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTemplate(Long templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new RuntimeException("Template not found with id: " + templateId);
        }
        templateRepository.deleteById(templateId);
        log.info("Deleted notification template with id: {}", templateId);
    }

    @Override
    public NotificationTemplateResponse toggleTemplateStatus(Long templateId, boolean isActive) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        template.setIsActive(isActive);
        NotificationTemplate saved = templateRepository.save(template);
        log.info("Toggled template status for {}: {}", saved.getTemplateName(), isActive);
        return mapToResponse(saved);
    }

    @Override
    public String processTemplate(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || variables == null) {
            return templateContent;
        }

        String result = templateContent;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    @Override
    public boolean validateTemplate(String templateContent) {
        if (templateContent == null || templateContent.trim().isEmpty()) {
            return false;
        }

        // Basic validation - check for balanced braces
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");
        Matcher matcher = pattern.matcher(templateContent);
        
        // Check if all placeholders are properly formatted
        while (matcher.find()) {
            String placeholder = matcher.group();
            if (!placeholder.matches("\\{\\{\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*\\}\\}")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> extractTemplateVariables(String templateContent) {
        if (templateContent == null) {
            return List.of();
        }

        Pattern pattern = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");
        Matcher matcher = pattern.matcher(templateContent);
        
        return matcher.results()
                .map(matchResult -> matchResult.group(1))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public NotificationTemplateResponse cloneTemplate(Long templateId, String newTemplateName) {
        NotificationTemplate original = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        NotificationTemplate cloned = new NotificationTemplate();
        cloned.setTemplateName(newTemplateName);
        cloned.setTemplateType(original.getTemplateType());
        cloned.setSubject(original.getSubject());
        cloned.setContent(original.getContent());
        cloned.setLanguage(original.getLanguage());
        cloned.setIsActive(false); // New cloned templates start as inactive
        cloned.setDescription("Cloned from: " + original.getTemplateName());

        NotificationTemplate saved = templateRepository.save(cloned);
        log.info("Cloned template {} to {}", original.getTemplateName(), newTemplateName);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTemplateUsageStats(Long templateId) {
        // Basic implementation - can be enhanced with actual usage tracking
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        return Map.of(
                "templateId", template.getId(),
                "templateName", template.getTemplateName(),
                "isActive", template.getIsActive(),
                "createdAt", template.getCreatedAt(),
                "updatedAt", template.getUpdatedAt(),
                "usageCount", 0L // Placeholder - implement actual usage tracking
        );
    }

    @Override
    public void initializeDefaultTemplates() {
        log.info("Initializing default notification templates...");
        
        // Create default templates if they don't exist
        createDefaultTemplateIfNotExists("welcome_email", NotificationTemplate.TemplateType.EMAIL, 
                "Welcome to School Management System", 
                "Welcome {{userName}}! Your account has been created successfully.", "en");
        
        createDefaultTemplateIfNotExists("password_reset", NotificationTemplate.TemplateType.EMAIL,
                "Password Reset Request",
                "Hello {{userName}}, please click the link to reset your password: {{resetLink}}", "en");
        
        createDefaultTemplateIfNotExists("grade_notification", NotificationTemplate.TemplateType.EMAIL,
                "New Grade Posted",
                "Hello {{studentName}}, a new grade has been posted for {{courseName}}: {{grade}}", "en");

        log.info("Default templates initialization completed");
    }

    private void createDefaultTemplateIfNotExists(String name, NotificationTemplate.TemplateType type, 
                                                  String subject, String content, String language) {
        boolean exists = templateRepository.findAll().stream()
                .anyMatch(t -> t.getTemplateName().equals(name) && 
                             t.getTemplateType().equals(type) && 
                             t.getLanguage().equals(language));
        
        if (!exists) {
            NotificationTemplate template = new NotificationTemplate();
            template.setTemplateName(name);
            template.setTemplateType(type);
            template.setSubject(subject);
            template.setContent(content);
            template.setLanguage(language);
            template.setIsActive(true);
            template.setDescription("Default system template");
            templateRepository.save(template);
            log.info("Created default template: {}", name);
        }
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate template) {
        NotificationTemplateResponse response = new NotificationTemplateResponse();
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateType(template.getTemplateType());
        response.setSubject(template.getSubject());
        response.setContent(template.getContent());
        response.setLanguage(template.getLanguage());
        response.setIsActive(template.getIsActive());
        response.setDescription(template.getDescription());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        // Set other fields to defaults or null as needed
        return response;
    }
}
