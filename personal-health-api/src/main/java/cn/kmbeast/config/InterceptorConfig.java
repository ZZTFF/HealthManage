package cn.kmbeast.config;

import cn.kmbeast.Interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API拦截器配置
 */
//实现WebMvcConfigurer接口,将自定义的JwtInterceptor拦截器注册到springMVC请求处理流程
//表明这是一个配置类，Spring 会将其识别为配置并加载到应用上下文中。
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Value("${my-server.api-context-path}")
    private String API;

//    重写addInterceptors方法
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器注册
        registry.addInterceptor(new JwtInterceptor())
//                先拦截所有的请求路径
                .addPathPatterns("/**")
                // 放行登录、注册请求,下面这些请求是在进入JwtInterceptor之前就已经放行的请求
                .excludePathPatterns(
                        API + "/user/login",
                        API + "/user/register",
                        API + "/file/upload",
                        API + "/file/getFile"
                );
    }
}
