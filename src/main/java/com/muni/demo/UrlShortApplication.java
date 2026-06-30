package com.muni.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrlShortApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortApplication.class, args);
	}

}
