package com.bachld.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${storage.screenshot-dir:uploads/screenshots}")
    @NonFinal
    String screenshotDir;

    @Value("${storage.screenshot-url-path:/resources/images/screenshots}")
    @NonFinal
    String screenshotUrlPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPattern = normalizeUrlPath(screenshotUrlPath) + "/**";
        String location = Path.of(screenshotDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlPattern).addResourceLocations(location);
    }

    private String normalizeUrlPath(String value) {
        if (value == null || value.isBlank()) {
            return "/resources/images/screenshots";
        }
        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
