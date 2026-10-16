package com.ecommerce.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CatalogoApplication {
    public static void main(String[] args) { SpringApplication.run(CatalogoApplication.class, args); }
}
