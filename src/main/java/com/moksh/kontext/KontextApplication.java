package com.moksh.kontext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class KontextApplication {

    public static void main(String[] args) {
        SpringApplication.run(KontextApplication.class, args);
    }

}
