package com.unitbv.myquiz.thy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class MyQuizThymeleafApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(MyQuizThymeleafApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MyQuizThymeleafApplication.class);
    }
}

