package com.github.cuzfrog.task.pipeline;

import com.github.cuzfrog.task.domain.MySrcEvent;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
final class SimpleUniqueVerificationSink implements SinkFunction<MySrcEvent> {
    private final PrintSinkFunction<MySrcEvent> printSinkFunction = new PrintSinkFunction<>();
    private final Set<Long> idsBuffer = new HashSet<>(); // put in heap for simplicity with parallelism=1


    @Override
    public void invoke(MySrcEvent value, Context context) throws Exception {
        printSinkFunction.invoke(value, context);
        if (idsBuffer.contains(value.getId())) {
            throw new AssertionError("Found duplicate:" + value);
        }
        idsBuffer.add(value.getId());
    }
}
