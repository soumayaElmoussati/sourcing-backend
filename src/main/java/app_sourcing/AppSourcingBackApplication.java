package app_sourcing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // ← active les @Scheduled dans OAuthOrderSyncJob
public class AppSourcingBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppSourcingBackApplication.class, args);
    }
}
