package com.unitbv.myquiz.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MyQuizIamApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyQuizIamApplication.class, args);
	}

}
