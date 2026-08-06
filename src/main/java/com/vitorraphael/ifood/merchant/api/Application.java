package com.vitorraphael.ifood.merchant.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@EnableScheduling
@SpringBootApplication
public class Application {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(Application.class, args);
		abrirNoEdge("http://localhost:8080");
	}

	// Usa o protocolo "microsoft-edge:" que o Windows ja registra sozinho,
	// em vez de Desktop.browse() (que abriria o navegador padrao do usuario,
	// nao necessariamente o Edge). Se por algum motivo isso falhar (Windows
	// muito antigo, Edge desinstalado), cai no navegador padrao mesmo.
	private static void abrirNoEdge(String url) {
		try {
			new ProcessBuilder("cmd", "/c", "start", "microsoft-edge:" + url).start();
		} catch (IOException e) {
			try {
				System.setProperty("java.awt.headless", "false");
				java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
			} catch (IOException ignorado) {
				// sem navegador disponivel — o usuario ainda pode acessar http://localhost:8080 manualmente
			}
		}
	}
}