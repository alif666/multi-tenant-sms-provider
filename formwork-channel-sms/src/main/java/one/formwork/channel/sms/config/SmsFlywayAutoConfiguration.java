package one.formwork.channel.sms.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
public class SmsFlywayAutoConfiguration {

    @Bean(name = "csFlyway", initMethod = "migrate")
    @ConditionalOnBean(DataSource.class)
    public Flyway csFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/cs")
                .load();
    }
}
