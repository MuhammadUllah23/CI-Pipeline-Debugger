package com.muhammadullah.ci_debugger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", matchIfMissing = true)
public class CiDebuggerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CiDebuggerApplication.class, args);
	}

}
