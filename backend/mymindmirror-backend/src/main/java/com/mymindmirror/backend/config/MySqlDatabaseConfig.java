package com.mymindmirror.backend.config;

import com.mymindmirror.backend.config.properties.MySqlDataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
@EnableJpaRepositories(
        basePackages = "com.mymindmirror.backend.repository",  // your JPA repositories package
        entityManagerFactoryRef = "mysqlEntityManagerFactory",
        transactionManagerRef = "mysqlTransactionManager"
)
public class MySqlDatabaseConfig {

    @Bean
    @Primary
    public DataSource mysqlDataSource(MySqlDataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.url());
        ds.setUsername(properties.username());
        ds.setPassword(properties.password());
        ds.setDriverClassName(properties.driverClassName());
        ds.setMaximumPoolSize(10);
        ds.setConnectionTimeout(5000);
        ds.setValidationTimeout(3000);
        ds.setMaxLifetime(240000);
        return ds;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean mysqlEntityManagerFactory(
            @Qualifier("mysqlDataSource") DataSource dataSource,
            JpaProperties jpaProperties,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.mymindmirror.backend.model")  // your entity package
                .properties(jpaProperties.getProperties())
                .persistenceUnit("mysql")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager mysqlTransactionManager(
            @Qualifier("mysqlEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}