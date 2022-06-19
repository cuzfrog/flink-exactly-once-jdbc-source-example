package com.github.cuzfrog.task;

import com.github.cuzfrog.task.domain.MySrcEvent;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
final class DataStreamApp implements ApplicationRunner {
    private final SourceFunction<List<MySrcEvent>> source;
    private final SinkFunction<MySrcEvent> sink;

    DataStreamApp(SourceFunction<List<MySrcEvent>> source, SinkFunction<MySrcEvent> sink) {
        this.source = source;
        this.sink = sink;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1)
                .addSource(source).uid("db-source")
                .flatMap((value, out) -> value.forEach(out::collect), TypeInformation.of(MySrcEvent.class)).uid("flat-map")
                .addSink(sink).uid("sink");
        env.execute("flink-test-app");
    }
}
