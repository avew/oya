package io.github.avew.oya.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenAiConfig implements WebMvcConfigurer {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Bean
    public OpenAiService openAiService() {
        if (openAiApiKey == null || openAiApiKey.equals("your-api-key-here")) {
            // Return a mock service or handle the case where API key is not configured
            return null;
        }
        return new OpenAiService(openAiApiKey);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
