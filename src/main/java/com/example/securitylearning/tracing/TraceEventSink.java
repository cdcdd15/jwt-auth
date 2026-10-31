package com.example.securitylearning.tracing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

public class TraceEventSink {

    public static final String HEADER = "X-Correlation-ID";
    private static final String EVENTS = TraceEventSink.class.getName() + ".events";
    private static final int MAX_COMPLETED_TRACES = 128;
    private final Map<String, List<String>> completed = new LinkedHashMap<>();

    public String begin(HttpServletRequest request) {
        request.setAttribute(EVENTS, new ArrayList<String>());
        return UUID.randomUUID().toString();
    }

    public void record(HttpServletRequest request, String event) {
        currentEvents(request).add(event);
    }

    public synchronized void complete(HttpServletRequest request, String id) {
        completed.put(id, List.copyOf(currentEvents(request)));
        request.removeAttribute(EVENTS);
        if (completed.size() > MAX_COMPLETED_TRACES) {
            completed.remove(completed.keySet().iterator().next());
        }
    }

    public synchronized List<String> events(String id) {
        return completed.getOrDefault(id, List.of());
    }

    @SuppressWarnings("unchecked")
    private List<String> currentEvents(HttpServletRequest request) {
        return (List<String>) request.getAttribute(EVENTS);
    }
}
