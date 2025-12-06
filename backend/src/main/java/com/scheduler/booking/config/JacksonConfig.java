package com.scheduler.booking.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Disable default timestamp serialization (which uses array format)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register JavaTimeModule for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());

        // Hibernate5Module to properly serialize Hibernate lazy-loaded entities
        Hibernate5JakartaModule hibernate5Module = new Hibernate5JakartaModule();
        hibernate5Module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true);
        mapper.registerModule(hibernate5Module);

        // Custom module to serialize LocalDateTime as epoch milliseconds
        SimpleModule epochModule = new SimpleModule();
        epochModule.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    // Convert LocalDateTime to epoch milliseconds using system default timezone
                    long epochMilli = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    gen.writeNumber(epochMilli);
                }
            }
        });
        mapper.registerModule(epochModule);

        return mapper;
    }
}
