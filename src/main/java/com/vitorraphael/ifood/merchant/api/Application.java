package com.vitorraphael.ifood.merchant.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@EnableScheduling
@SpringBootApplication
public class Application {

	public static void main(String[] args) throws IOException {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(Application.class, args);
		java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://localhost:8080"));
	}
}