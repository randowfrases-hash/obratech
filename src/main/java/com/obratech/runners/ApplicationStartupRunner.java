package com.obratech.runners;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.obratech.service.SeedService;

@Component
public class ApplicationStartupRunner implements ApplicationRunner {

    private final SeedService seedService;

    public ApplicationStartupRunner(SeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {

        System.out.println(" Iniciando sistema...");

        seedService.initSystem();

        System.out.println(" Sistema listo");
    }
}
