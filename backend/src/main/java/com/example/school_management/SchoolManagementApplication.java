package com.example.school_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class SchoolManagementApplication  {


	public static void main(String[] args) {
		SpringApplication.run(SchoolManagementApplication.class, args);
	}

	
}
