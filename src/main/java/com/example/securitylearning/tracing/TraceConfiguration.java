package com.example.securitylearning.tracing;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class TraceConfiguration {

    @Bean
    TraceEventSink traceEventSink() {
        return new TraceEventSink();
    }

    @Bean
    FilterRegistrationBean<RequestTraceFilter> requestTraceFilter(TraceEventSink sink) {
        var registration = new FilterRegistrationBean<>(new RequestTraceFilter(sink));
        registration.setName("requestTraceFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }

    @Bean
    WebMvcConfigurer traceMvcConfigurer(TraceEventSink sink) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new MvcTraceInterceptor(sink));
            }
        };
    }
}
