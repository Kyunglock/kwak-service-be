package com.investment.stockadvisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.investment.stockadvisor", "kwak.common"})
public class StockAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockAdvisorApplication.class, args);
	}

}
