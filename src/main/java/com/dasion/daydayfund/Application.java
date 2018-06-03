package com.dasion.daydayfund;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;



@SpringBootApplication
@ImportResource(locations={"classpath:quartz-config.xml"})
public class Application 
//extends SpringBootServletInitializer {
//	    @Override
//	    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
//	        return application.sources(Application.class);
//	    }
//	}
{
	public static void main(String[] args){
		SpringApplication.run(Application.class, args);
	}
}
