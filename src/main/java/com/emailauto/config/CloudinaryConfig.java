package com.emailauto.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {
    @Bean
    @ConditionalOnExpression("'${app.cloudinary.cloud-name:}' != '' && '${app.cloudinary.api-key:}' != '' && '${app.cloudinary.api-secret:}' != ''")
    Cloudinary cloudinary(AppProperties properties) {
        AppProperties.Cloudinary config = properties.getCloudinary();
        if (!StringUtils.hasText(config.getCloudName()) || !StringUtils.hasText(config.getApiKey()) || !StringUtils.hasText(config.getApiSecret())) {
            throw new IllegalStateException("Cloudinary credentials are required");
        }
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", config.getCloudName(),
                "api_key", config.getApiKey(),
                "api_secret", config.getApiSecret(),
                "secure", true));
    }
}
