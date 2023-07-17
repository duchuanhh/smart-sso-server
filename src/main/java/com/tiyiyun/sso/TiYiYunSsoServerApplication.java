package com.tiyiyun.sso;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableScheduling
//@EnableRedisHttpSession
@SpringBootApplication
@MapperScan("com.tiyiyun.repository")
public class TiYiYunSsoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiYiYunSsoServerApplication.class, args);
    }
    
	@Bean
	public RestTemplate restTemplate() {
		//切换RestTemplate底层调用为OkHttp,因为OkHttp的性能比较优越
	    RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
	    //配置消息转换器，处理所有响应乱码
	    List<HttpMessageConverter<?>> httpMessageConverter = restTemplate.getMessageConverters();
	    //设置编码
	    httpMessageConverter.set(1,new StringHttpMessageConverter(StandardCharsets.UTF_8));
	    restTemplate.setMessageConverters(httpMessageConverter);
	    return restTemplate;

	}

}
