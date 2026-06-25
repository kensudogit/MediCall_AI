package com.medicall.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Railway Postgres の DATABASE_URL を JDBC 接続情報に変換する。
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Pattern DATABASE_URL_PATTERN = Pattern.compile(
            "^postgres(?:ql)?://([^:]+):([^@]*)@([^:/]+)(?::(\\d+))?/(.+)$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_PRIVATE_URL")
        );
        if (databaseUrl == null) {
            return;
        }

        try {
            Matcher matcher = DATABASE_URL_PATTERN.matcher(databaseUrl.trim());
            if (!matcher.matches()) {
                System.err.println("[railway] Could not parse DATABASE_URL");
                return;
            }

            String username = decode(matcher.group(1));
            String password = decode(matcher.group(2));
            String host = matcher.group(3);
            String port = matcher.group(4) != null ? matcher.group(4) : "5432";
            String database = matcher.group(5).replaceAll("/.*$", "");

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + database);
            props.put("spring.datasource.username", username);
            props.put("spring.datasource.password", password);

            environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabase", props));
            System.err.println("[railway] Configured datasource for host: " + host);
        } catch (Exception ex) {
            System.err.println("[railway] Failed to parse DATABASE_URL: " + ex.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
