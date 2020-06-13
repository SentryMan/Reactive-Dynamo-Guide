package com.jojo.reactive.dynamo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReactiveDynamoExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReactiveDynamoExampleApplication.class, args);
  }

  @Bean
  public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
    return new NettyReactiveWebServerFactory();
  }
}
