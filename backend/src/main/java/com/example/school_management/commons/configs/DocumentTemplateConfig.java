package com.example.school_management.commons.configs;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class DocumentTemplateConfig {
    @Bean
    SpringResourceTemplateResolver documentTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setResolvablePatterns(Set.of("grades/grade-sheet", "timetable/timetable-export-pdf"));
        resolver.setOrder(0);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
