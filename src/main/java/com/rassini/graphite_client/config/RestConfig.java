package com.rassini.graphite_client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestConfig {

    @Bean
    public ObjectMapper graphiteObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ CLAVE: permitir "" -> null
        mapper.coercionConfigDefaults()
            .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);

        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(ObjectMapper graphiteObjectMapper) {

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();

        jacksonConverter.setObjectMapper(graphiteObjectMapper);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(jacksonConverter));

        return restTemplate;
    }
}