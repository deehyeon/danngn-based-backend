package backend.daangnbasedbackend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public WebMvcConfigurer localStorageResourceHandler(
            @Value("${storage.local.base-path:./uploads}") String basePath,
            @Value("${storage.local.base-url:http://localhost:8080/files}") String baseUrl) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String urlPath = URI.create(baseUrl).getPath();
                registry.addResourceHandler(urlPath + "/**")
                        .addResourceLocations("file:" + basePath + "/");
            }
        };
    }
}
