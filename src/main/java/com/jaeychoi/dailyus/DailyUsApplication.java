package com.jaeychoi.dailyus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DailyUsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailyUsApplication.class, args);
	}

}
