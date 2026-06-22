package com.investment.survey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.investment.survey", "kwak.common"})
public class SurveyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurveyServiceApplication.class, args);
	}

}
