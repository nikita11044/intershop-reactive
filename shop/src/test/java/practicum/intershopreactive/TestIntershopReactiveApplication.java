package practicum.intershopreactive;

import org.springframework.boot.SpringApplication;

public class TestIntershopReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.from(IntershopReactiveApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
