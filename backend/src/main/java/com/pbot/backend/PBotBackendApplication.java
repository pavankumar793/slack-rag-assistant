package com.pbot.backend;

import com.pbot.backend.config.RagProperties;
import com.pbot.backend.config.GitHubModelsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ RagProperties.class, GitHubModelsProperties.class })
public class PBotBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PBotBackendApplication.class, args);
	}

}
