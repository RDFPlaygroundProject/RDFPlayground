package api

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
open class WebConfig: WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        // Based on https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-cors-global
        registry.addMapping("/api/**")
            .allowedOrigins("https://rdfplayground.dcc.uchile.cl")
            .allowedMethods("GET", "POST")
            .maxAge(3600)
    }
}
