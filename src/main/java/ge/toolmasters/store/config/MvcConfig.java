package ge.toolmasters.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ეს იღებს შენი პროექტის ძირში არსებულ "uploads" ფოლდერის ზუსტ მისამართს
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // ბრაუზერს ვეუბნებით: თუ მოგთხოვენ /uploads/ragaca.jpg, მოძებნე ის ჩვენს ფიზიკურ ფოლდერში!
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}
