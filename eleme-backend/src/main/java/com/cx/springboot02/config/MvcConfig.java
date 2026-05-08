package com.cx.springboot02.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MvcConfig implements WebMvcConfigurer{

    @Value("${file.staticAccessPath}")
    private String staticAccessPath;
    @Value("${file.uploadFolder}")
    private String uploadFolder;
    @Value("${file.uploadImage}")
    private String uploadImage;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("file:" + uploadFolder + uploadImage);
        registry.addResourceHandler(staticAccessPath).addResourceLocations("file:" + uploadFolder);
    }

    @Autowired
    private TokenInterceptor tokenInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry){
        //拦截所有的路径请求
        registry
                .addInterceptor(tokenInterceptor)
                .addPathPatterns("/**");  // token 验证拦截器
//                .excludePathPatterns("/alipay/**")
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")//设置允许跨域的路径
                .allowedOrigins("http://127.0.0.1:3000",
                        "http://localhost:3000",
                        "http://192.168.137.1:3000",
                        "http://192.168.50.236:3000",
                        "http://localhost:9528",//后台和商家
                        "http://localhost:9528",
                        "http://localhost:8090",
                        "http://127.0.0.1:8090",
                        "http://cx-eleme.natapp1.cc:3030",
                        "http://cx-eleme.natapp1.cc"
                        )//设置允许跨域请求的域名
                .allowCredentials(true)//是否允许证书 不再默认开启
                .allowedMethods("*")//设置允许的方法
                .maxAge(3600);//跨域允许时间
    }



    /**
     * 设置返回的json
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                //json格式输出
                SerializerFeature.PrettyFormat,
                // 保留map为空的字段
                SerializerFeature.WriteMapNullValue,
                // 将String类型的null转成""形式
                SerializerFeature.WriteNullStringAsEmpty,
                // 将Number类型的null转成0
                SerializerFeature.WriteNullNumberAsZero,
                // 将List类型的null转成[],而不是""
                SerializerFeature.WriteNullListAsEmpty,
                // Boolean类型的null转成false
                SerializerFeature.WriteNullBooleanAsFalse,
                // 处理可能循环引用的问题
                SerializerFeature.DisableCircularReferenceDetect);
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        List<MediaType> mediaTypeList = new ArrayList<>();
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(mediaTypeList);
        converters.add(converter);
    }



}
