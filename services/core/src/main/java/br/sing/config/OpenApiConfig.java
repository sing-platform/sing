package br.sing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("SING API")
                        .version("1.0")
                        .description("API do SING — sistema de gestão acadêmica, administrativa, financeira e psicossocial."))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }



    @Component
    public class SwaggerUrlLogger implements ApplicationListener<ApplicationReadyEvent> {

        @Value("${server.port}")
        private String port;

        @Value("${server.servlet.context-path:}")
        private String contextPath;

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            System.out.println("\n--------------------------------------------");
            System.out.println("Swagger UI: http://localhost:" + port + contextPath + "/swagger-ui/index.html");
            System.out.println("--------------------------------------------\n");
        }
    }


    //abre apenas em perfil dev ativar no application.yml
//    @Component
//    @Profile("dev")
//    public class SwaggerAutoOpen implements ApplicationListener<ApplicationReadyEvent> {
//
//        @Value("${server.port}")
//        private String port;
//
//        @Value("${server.servlet.context-path:}")
//        private String contextPath;
//
//        @Override
//        public void onApplicationEvent(ApplicationReadyEvent event) {
//            String url = "http://localhost:" + port + contextPath + "/swagger-ui/index.html";
//            try {
//                String os = System.getProperty("os.name").toLowerCase();
//                Runtime runtime = Runtime.getRuntime();
//
//                if (os.contains("win")) {
//                    runtime.exec(new String[]{"cmd", "/c", "start", url});
//                } else if (os.contains("mac")) {
//                    runtime.exec(new String[]{"open", url});
//                } else {
//                    runtime.exec(new String[]{"xdg-open", url});  // Linux
//                }
//            } catch (Exception e) {
//                System.out.println("\n Swagger UI: http://localhost:" + port + contextPath + "/swagger-ui/index.html\n");
//            }
//        }
//    }
}