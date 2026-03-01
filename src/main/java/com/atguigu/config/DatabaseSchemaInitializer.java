package com.atguigu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Value("${app.schema-auto-init:true}")
    private boolean schemaAutoInit;

    public DatabaseSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!schemaAutoInit) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/user_schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/social_schema.sql"));
        }
    }
}
