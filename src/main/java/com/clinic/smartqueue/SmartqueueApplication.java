package com.clinic.smartqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//Starting point for application to run.

/*
Earlier it was-
Controller → Repository → Database ❌ (not ideal)
We  improved it to:
Controller → Service → Repository → Database ✅
 */
@SpringBootApplication
public class SmartqueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartqueueApplication.class, args);
    }

}
