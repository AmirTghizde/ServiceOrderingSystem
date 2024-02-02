package com.Maktab101.SpringProject;


import com.Maktab101.SpringProject.service.*;
import com.Maktab101.SpringProject.utils.CustomException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@SuppressWarnings("unused")
public class SpringProjectApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringProjectApplication.class, args);
    }
}
