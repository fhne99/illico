package fr.kernelis.illico;

import org.springframework.boot.SpringApplication;

public class TestIllicoApplication {

	public static void main(String[] args) {
		SpringApplication.from(IllicoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
