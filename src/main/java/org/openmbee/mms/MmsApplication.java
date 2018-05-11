package org.openmbee.mms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@EnableSwagger2
@SpringBootApplication public class MmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmsApplication.class, args);
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
            "MMS ReST API",
            "Some custom description of API.",
            "3.4.0",
            null,
            new Contact("Jason Han", "mms.openmbee.org", "jason.han@jpl.nasa.gov"),
            "APACHE 2.0", "https://www.apache.org/licenses/LICENSE-2.0", Collections.emptyList());
    }
}