package com.rassini.graphite_client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@Profile("!dev & !local")
public class RestConfig {

    @Bean
    public ObjectMapper graphiteObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Permitir "" -> null
        mapper.coercionConfigDefaults()
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);

        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(@NonNull ObjectMapper graphiteObjectMapper) {

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();

        jacksonConverter.setObjectMapper(graphiteObjectMapper);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(jacksonConverter));

        return restTemplate;
    }
}