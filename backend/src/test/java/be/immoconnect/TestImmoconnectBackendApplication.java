package be.immoconnect;

import org.springframework.boot.SpringApplication;

public class TestImmoconnectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(ImmoconnectBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
