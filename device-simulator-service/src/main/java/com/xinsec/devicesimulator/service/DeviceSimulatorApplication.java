package com.xinsec.devicesimulator.service;

import org.springframework.boot.SpringApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xinsec.devicesimulator.service.mapper")
public class DeviceSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceSimulatorApplication.class, args);
    }

}
