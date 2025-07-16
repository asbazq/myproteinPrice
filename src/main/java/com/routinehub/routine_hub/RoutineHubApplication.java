package com.routinehub.routine_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoutineHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoutineHubApplication.class, args);
	}

}
