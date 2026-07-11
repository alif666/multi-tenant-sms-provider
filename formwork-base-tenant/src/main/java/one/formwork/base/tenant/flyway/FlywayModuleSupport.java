package one.formwork.base.tenant.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public final class FlywayModuleSupport {

    private FlywayModuleSupport() {
    }

    public static Flyway create(DataSource dataSource, String module) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/" + module)
                .load();
    }
}
