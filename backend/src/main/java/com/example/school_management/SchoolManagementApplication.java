package com.example.school_management;

import com.example.school_management.commons.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;

@SpringBootApplication
@EnableAsync
public class SchoolManagementApplication implements CommandLineRunner {

	@Autowired
	private EmailService emailService;

	public static void main(String[] args) {
		SpringApplication.run(SchoolManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// 1. Prepare template variables
		Map<String, Object> vars = Map.of(
				"name", "Test User",
				"code", "XYZ789"
		);

		// 2. Send the test email
		emailService.sendTemplateEmail(
				"haythemmouna@gmail.com",   // ← your test address
				"📧 Test OTP Email",
				"otp",                     // ← picks up otp.html in templates/email/
				vars
		);

		// 3. Give the @Async task a moment to execute (5s)
		Thread.sleep(5_000);

		System.out.println("➡️ Test email attempted. Check your inbox/logs.");

		// 4. If you want the app to exit immediately after the test, uncomment:
		// System.exit(0);
	}

}
