package com.eman.user_service.tenancy;

import com.eman.user_service.model.Tenant;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for multi-tenant data sources
 * Manages dynamic creation and routing of tenant-specific data sources
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TenantDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String defaultUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${taskmanager.tenancy.shared-table-schema:shared}")
    private String sharedSchema;

    @Value("${taskmanager.tenancy.default-tenant-schema-prefix:tenant_}")
    private String schemaPrefix;

    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    private RoutingDataSource routingDataSource;

    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    @Bean
    public DataSource masterDataSource() {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(defaultUrl + "?currentSchema=" + sharedSchema);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public RoutingDataSource dataSource(){

        RoutingDataSource routing = new RoutingDataSource();

        DataSource defaultDs = createDefaultDataSource();

        Map<Object,Object> targets = new HashMap<>();
        targets.put("default", defaultDs);

        routing.setDefaultTargetDataSource(defaultDs);
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();

        this.routingDataSource = routing;

        return routing;
    }
    @Bean
    public JdbcTemplate jdbcTemplate(
            @Qualifier("masterDataSource") DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }
    /**
     * Create default data source for shared schema
     */
    private DataSource createDefaultDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(defaultUrl + "?currentSchema=" + sharedSchema);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setPoolName("default-hikari-pool");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        log.info("Creating default datasource for schema: {}", sharedSchema);
        return new HikariDataSource(config);
    }

    /**
     * Create data source for a specific tenant schema
     */
    public DataSource createDataSourceForTenant(String schemaName) {
        return tenantDataSources.computeIfAbsent(schemaName, name -> {
            log.info("Creating new datasource for tenant schema: {}", name);

            HikariConfig config = new HikariConfig();
            String urlWithSchema = defaultUrl + "?currentSchema=" + name;
            config.setJdbcUrl(urlWithSchema);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);
            config.setPoolName("hikari-pool-" + name);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");

            return new HikariDataSource(config);
        });
    }
    @EventListener(ApplicationReadyEvent.class)
    public void loadTenantsAfterStartup() {

        Map<Object,Object> targets = new HashMap<>();

        targets.put(
                "default",
                createDefaultDataSource()
        );


        List<Tenant> tenants =
                jdbcTemplate.query(
                        "select * from tenants where status='ACTIVE'",
                        (rs,row)-> {
                            Tenant t = new Tenant();
                            t.setSchemaName(
                                    rs.getString("schema_name")
                            );
                            return t;
                        }
                );


        for(Tenant tenant: tenants){

            targets.put(
                    tenant.getSchemaName(),
                    createDataSourceForTenant(
                            tenant.getSchemaName()
                    )
            );
        }


        routingDataSource.setTargetDataSources(targets);
        routingDataSource.afterPropertiesSet();
    }
    /**
     * Load active tenants from database
     */
    private void loadActiveTenants(Map<Object, Object> targetDataSources) {
        try {
            List<Tenant> activeTenants = findAllActiveTenants();

            for (Tenant tenant : activeTenants) {
                DataSource ds = createDataSourceForTenant(tenant.getSchemaName());
                targetDataSources.put(tenant.getSchemaName(), ds);
                log.info("Loaded active tenant: {}", tenant.getSchemaName());
            }
        } catch (Exception e) {
            log.warn("Could not load active tenants from database: {}", e.getMessage());
            // Table might not exist yet - that's okay
        }
    }

    private List<Tenant> findAllActiveTenants() {
        return jdbcTemplate.query(
                "select * from tenants where status='ACTIVE'",
                (rs, rowNum) -> Tenant.builder()
                        .schemaName(rs.getString("schema_name"))
                        .build()
        );
    }

    /**
     * Dynamically add a new tenant data source
     */
    public void addTenantDataSource(String schemaName) {
        if (routingDataSource == null) {
            log.error("RoutingDataSource is not initialized");
            return;
        }

        DataSource newDataSource = createDataSourceForTenant(schemaName);
        routingDataSource.addTargetDataSource(schemaName, newDataSource);

        log.info("Dynamically added datasource for tenant: {}", schemaName);
    }

    /**
     * Remove a tenant data source
     */
    public void removeTenantDataSource(String schemaName) {
        if (routingDataSource == null) {
            log.error("RoutingDataSource is not initialized");
            return;
        }

        if (routingDataSource.hasTargetDataSource(schemaName)) {
            routingDataSource.removeTargetDataSource(schemaName);

            // Close connection pool
            DataSource removedDS = tenantDataSources.remove(schemaName);
            if (removedDS instanceof HikariDataSource hikariDS) {
                hikariDS.close();
            }

            log.info("Removed datasource for tenant: {}", schemaName);
        }
    }

    /**
     * Refresh all data sources (sync with database)
     */
   // @EventListener(ApplicationReadyEvent.class)
    public void refreshAllDataSources() {
        if (routingDataSource == null) {
            return;
        }

        try {
            List<Tenant> activeTenants = findAllActiveTenants();

            Map<Object, Object> targetDataSources = new HashMap<>();
            targetDataSources.put("default", createDefaultDataSource());

            for (Tenant tenant : activeTenants) {
                DataSource ds = createDataSourceForTenant(tenant.getSchemaName());
                targetDataSources.put(tenant.getSchemaName(), ds);
            }

            routingDataSource.setTargetDataSources(targetDataSources);
            routingDataSource.afterPropertiesSet();

            log.info("Refreshed all data sources. Active tenants: {}", activeTenants.size());
        } catch (Exception e) {
            log.error("Failed to refresh data sources: {}", e.getMessage());
        }
    }

    /**
     * Check if tenant data source exists
     */
    public boolean hasTenantDataSource(String schemaName) {
        return tenantDataSources.containsKey(schemaName);
    }
}
