package com.stadtmeldung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import database.initialization.DatabaseInitializer;

@SpringBootApplication
public class StadtmeldungApplication {

    public static void main(String[] args) {

        // Bestehende SQLite-Datenbank initialisieren
        DatabaseInitializer.initialize();

        // Spring Boot und Webserver starten
        SpringApplication.run(StadtmeldungApplication.class, args);
    }
}
