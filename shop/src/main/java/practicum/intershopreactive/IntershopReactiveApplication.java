package practicum.intershopreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class IntershopReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntershopReactiveApplication.class, args);
    }

}
