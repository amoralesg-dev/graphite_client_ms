package com.rassini.graphite_client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
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

    // ✅ 🔥 DESACTIVA VALIDACIÓN SSL (SIN DEPENDENCIAS)
    @PostConstruct
    public void disableSslValidation() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // no-op
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // no-op
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            // ✅ aplica a TODAS las conexiones HTTPS
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // ✅ ignora hostname
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new RuntimeException("Error desactivando SSL", e);
        }
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