package com.example.securitylearning.tracing;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class MvcTraceInterceptor implements HandlerInterceptor {

    private final TraceEventSink sink;

    public MvcTraceInterceptor(TraceEventSink sink) {
        this.sink = sink;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        record(request, "interceptor.preHandle");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        record(request, "interceptor.postHandle");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        record(request, "interceptor.afterCompletion");
    }

    private void record(HttpServletRequest request, String event) {
        if (request.getDispatcherType() == DispatcherType.REQUEST) {
            sink.record(request, event);
        }
    }
}
