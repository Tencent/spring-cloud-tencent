package com.tencent.cloud.tsf.msgw.scg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.tsf.annotation.EnableTsf;

/**
 * @author seanlxliu
 */
@SpringBootApplication
@EnableTsf
public class ScgApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScgApplication.class, args);
	}
}
