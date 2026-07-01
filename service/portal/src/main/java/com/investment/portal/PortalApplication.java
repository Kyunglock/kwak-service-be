package com.investment.portal;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(scanBasePackages = {"com.investment", "kwak.common"})
@MapperScan(basePackages = "com.investment", annotationClass = Mapper.class,
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class PortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortalApplication.class, args);
	}

}
