package com.smarthire;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.smarthire.modules", annotationClass = Mapper.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class SmartHireApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHireApplication.class, args);
    }
}
