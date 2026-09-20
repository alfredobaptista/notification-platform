package com.github.alfredobaptista.notification;

import org.springframework.boot.SpringApplication;

public class TestNotificationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(NotificationPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
