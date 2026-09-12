package com.docstorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class OnlineDocumentStorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineDocumentStorerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("\n=========================================");
        System.out.println("  Online Document Storer - STARTED");
        System.out.println("  URL: http://localhost:8080");
        System.out.println("  Swagger: http://localhost:8080/swagger-ui.html");
        System.out.println("=========================================\n");
    }
}
