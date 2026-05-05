package com.rassini.graphite_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GraphiteClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(GraphiteClientApplication.class, args);
	}

}
