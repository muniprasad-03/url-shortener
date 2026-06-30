package com.muni.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrlShortApplication {

	public static void main(String[] args) {
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl == null) {
			databaseUrl = System.getenv("SPRING_DATASOURCE_URL");
		}
		if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
			try {
				String cleanUrl = databaseUrl.substring(databaseUrl.indexOf("://") + 3);
				String[] authAndHost = cleanUrl.split("@");
				if (authAndHost.length == 2) {
					String[] credentials = authAndHost[0].split(":");
					if (credentials.length == 2) {
						System.setProperty("spring.datasource.username", credentials[0]);
						System.setProperty("spring.datasource.password", credentials[1]);
					}
					String hostDb = authAndHost[1];
					String jdbcUrl = "jdbc:postgresql://" + hostDb;
					System.setProperty("spring.datasource.url", jdbcUrl);
				} else {
					String jdbcUrl = "jdbc:postgresql://" + cleanUrl;
					System.setProperty("spring.datasource.url", jdbcUrl);
				}
			} catch (Exception e) {
				System.err.println("Error parsing database URL: " + e.getMessage());
			}
		}
		SpringApplication.run(UrlShortApplication.class, args);
	}

}
