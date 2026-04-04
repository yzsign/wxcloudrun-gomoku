package com.tencent.wxcloudrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.gomoku.sync")
@EnableScheduling
public class WxCloudRunApplication {

  public static void main(String[] args) {
    SpringApplication.run(WxCloudRunApplication.class, args);
  }
}
