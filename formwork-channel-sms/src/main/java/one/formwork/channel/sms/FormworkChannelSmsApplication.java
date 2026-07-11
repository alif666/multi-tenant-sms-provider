package one.formwork.channel.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "one.formwork")
@ConfigurationPropertiesScan(basePackages = "one.formwork")
@EntityScan(basePackages = "one.formwork")
@EnableJpaRepositories(basePackages = "one.formwork")
public class FormworkChannelSmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormworkChannelSmsApplication.class, args);
    }
}
