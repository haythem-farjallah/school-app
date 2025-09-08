package com.example.school_management.feature.communication.repository;

import com.example.school_management.feature.communication.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long>, JpaSpecificationExecutor<NotificationTemplate> {

    // Find template by name
    Optional<NotificationTemplate> findByTemplateName(String templateName);

    // Find templates by type
    List<NotificationTemplate> findByTemplateType(NotificationTemplate.TemplateType templateType);

    // Find active templates
    List<NotificationTemplate> findByIsActiveTrue();

    // Find templates by type and active status
    List<NotificationTemplate> findByTemplateTypeAndIsActiveTrue(NotificationTemplate.TemplateType templateType);

    // Find templates by language
    List<NotificationTemplate> findByLanguage(String language);

    // Find templates by category
    List<NotificationTemplate> findByCategory(String category);

    // Find template by name and language
    Optional<NotificationTemplate> findByTemplateNameAndLanguage(String templateName, String language);

    // Find template by name, type and language
    Optional<NotificationTemplate> findByTemplateNameAndTemplateTypeAndLanguage(
            String templateName, 
            NotificationTemplate.TemplateType templateType, 
            String language
    );

    // Search templates by name or description
    @Query("SELECT t FROM NotificationTemplate t WHERE " +
           "(LOWER(t.templateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "t.isActive = true")
    List<NotificationTemplate> searchActiveTemplates(@Param("searchTerm") String searchTerm);

    // Count templates by type
    @Query("SELECT t.templateType, COUNT(t) FROM NotificationTemplate t WHERE t.isActive = true GROUP BY t.templateType")
    List<Object[]> countTemplatesByType();

    // Find templates by multiple categories
    @Query("SELECT t FROM NotificationTemplate t WHERE t.category IN :categories AND t.isActive = true")
    List<NotificationTemplate> findByCategoriesAndActiveTrue(@Param("categories") List<String> categories);
}
