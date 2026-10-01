package com.mymindmirror.backend.config;

import com.mymindmirror.backend.config.properties.MySqlDataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MySqlDatabaseConfigTest {

    private MySqlDatabaseConfig config;

    @Mock
    private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    @Mock
    private EntityManagerFactoryBuilder.Builder builderStage1;

    @Mock
    private EntityManagerFactoryBuilder.Builder builderStage2;

    @Mock
    private LocalContainerEntityManagerFactoryBean emfBean;

    @BeforeEach
    void setUp() {
        config = new MySqlDatabaseConfig();
    }

    @Test
    @DisplayName("mysqlDataSource() configures Hikari DataSource with correct properties")
    void mysqlDataSource_configuresPoolCorrectly() {
        MySqlDataSourceProperties props = new MySqlDataSourceProperties(
                "jdbc:mysql://localhost:3306/test_db",
                "root",
                "secret",
                "com.mysql.cj.jdbc.Driver"
        );

        DataSource ds = config.mysqlDataSource(props);

        assertThat(ds).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) ds;
        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:mysql://localhost:3306/test_db");
        assertThat(hikari.getUsername()).isEqualTo("root");
        assertThat(hikari.getPassword()).isEqualTo("secret");
        assertThat(hikari.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(10);

        hikari.close();
    }

    @Test
    @DisplayName("mysqlEntityManagerFactory() builds container entity manager factory bean")
    void mysqlEntityManagerFactory_buildsSuccessfully() {
        DataSource mockDs = mock(DataSource.class);
        JpaProperties jpaProps = new JpaProperties();
        jpaProps.setProperties(Collections.singletonMap("hibernate.dialect", "org.hibernate.dialect.MySQLDialect"));

        when(entityManagerFactoryBuilder.dataSource(any())).thenReturn(builderStage1);
        when(builderStage1.packages(anyString())).thenReturn(builderStage2);
        when(builderStage2.properties(any())).thenReturn(builderStage2);
        when(builderStage2.persistenceUnit(anyString())).thenReturn(builderStage2);
        when(builderStage2.build()).thenReturn(emfBean);

        LocalContainerEntityManagerFactoryBean result = config.mysqlEntityManagerFactory(mockDs, jpaProps, entityManagerFactoryBuilder);

        assertThat(result).isSameAs(emfBean);
    }

    @Test
    @DisplayName("mysqlTransactionManager() wraps EntityManagerFactory in JpaTransactionManager")
    void mysqlTransactionManager_wrapsFactory() {
        EntityManagerFactory mockEmf = mock(EntityManagerFactory.class);

        PlatformTransactionManager txManager = config.mysqlTransactionManager(mockEmf);

        assertThat(txManager).isInstanceOf(JpaTransactionManager.class);
        assertThat(((JpaTransactionManager) txManager).getEntityManagerFactory()).isSameAs(mockEmf);
    }
}