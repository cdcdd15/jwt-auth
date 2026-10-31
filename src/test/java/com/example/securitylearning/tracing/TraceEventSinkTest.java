package com.example.securitylearning.tracing;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceEventSinkTest {

    @Test
    void completedTracesAreBoundedAndRequestStateIsRemoved() {
        var sink = new TraceEventSink();
        String first = null;
        String latest = null;
        for (int index = 0; index < 129; index++) {
            var request = new MockHttpServletRequest();
            latest = sink.begin(request);
            if (index == 0) {
                first = latest;
            }
            sink.record(request, "event");
            sink.complete(request, latest);
            assertThat(request.getAttributeNames().hasMoreElements()).isFalse();
        }
        assertThat(sink.events(first)).isEmpty();
        assertThat(sink.events(latest)).containsExactly("event");
    }

    @Test
    void completedSnapshotCannotBeModifiedByReaders() {
        var sink = new TraceEventSink();
        var request = new MockHttpServletRequest();
        String id = sink.begin(request);
        sink.record(request, "event");
        sink.complete(request, id);
        List<String> snapshot = sink.events(id);
        assertThatThrownBy(() -> snapshot.add("injected"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(sink.events(id)).containsExactly("event");
    }
}
