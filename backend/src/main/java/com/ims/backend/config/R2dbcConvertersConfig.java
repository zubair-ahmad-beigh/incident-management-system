package com.ims.backend.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import java.util.Arrays;
import java.util.UUID;

/**
 * Registers custom R2DBC converters so that java.util.UUID fields
 * are stored/read as CHAR(36) strings in MySQL.
 *
 * Without these converters, the MySQL R2DBC driver throws:
 *   IllegalArgumentException: Cannot encode class java.util.UUID
 */
@Configuration
public class R2dbcConvertersConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(dialect, Arrays.asList(
                new UuidToStringConverter(),
                new StringToUuidConverter()
        ));
    }

    /** UUID → String  (write to DB) */
    @WritingConverter
    static class UuidToStringConverter implements Converter<UUID, String> {
        @Override
        public String convert(UUID source) {
            return source.toString();
        }
    }

    /** String → UUID  (read from DB) */
    @ReadingConverter
    static class StringToUuidConverter implements Converter<String, UUID> {
        @Override
        public UUID convert(String source) {
            return UUID.fromString(source);
        }
    }
}
