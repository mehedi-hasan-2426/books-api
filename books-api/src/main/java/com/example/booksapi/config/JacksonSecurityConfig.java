package com.example.booksapi.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonSecurityConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonSecurityCustomizer() {
        return builder -> builder.postConfigurer(mapper -> {
            // Prevents polymorphic deserialization gadget-chain attacks (CVE-2017-7525 class)
            mapper.deactivateDefaultTyping();
            // Restrict scalar-to-object coercion — reduces deserialization attack surface (Jackson 2.12+)
            mapper.coercionConfigDefaults()
                  .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                  .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                  .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}

