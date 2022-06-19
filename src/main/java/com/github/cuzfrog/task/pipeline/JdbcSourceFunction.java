package com.github.cuzfrog.task.pipeline;

import com.github.cuzfrog.task.domain.MySrcEvent;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/fault-tolerance/state/#stateful-source-functions
@Component
final class JdbcSourceFunction extends RichSourceFunction<List<MySrcEvent>> implements CheckpointedFunction {
    private final Supplier<DataSource> dataSourceSupplier;
    private final Supplier<ResultSetExtractor<List<MySrcEvent>>> resultSetExtractorSupplier;
    private final Duration readDelay;
    private final Duration readWindow;
    private transient DataSource dataSource;
    private transient ResultSetExtractor<List<MySrcEvent>> resultSetExtractor;
    private transient ListState<Long> beginningTimestampState;
    private transient long beginningTimestamp;
    private transient volatile boolean isRunning = false;

    JdbcSourceFunction(@Qualifier("dataSourceProvider") Supplier<DataSource> dataSourceSupplier,
                       @Qualifier("resultSetExtractorSupplier") Supplier<ResultSetExtractor<List<MySrcEvent>>> resultSetExtractorSupplier,
                       @Value("${app.db.read-delay.duration}") Duration readDelay,
                       @Value("${app.db.read-window.duration}") Duration readWindow) {
        if (!(dataSourceSupplier instanceof Serializable)) {
            throw new IllegalArgumentException("dataSourceSupplier must be serializable.");
        }
        if (!(resultSetExtractorSupplier instanceof Serializable)) {
            throw new IllegalArgumentException("resultSetExtractor must be serializable.");
        }
        if (readDelay.compareTo(readWindow) <= 0) {
            throw new IllegalArgumentException("readDelay must > readWindow");
        }
        this.dataSourceSupplier = dataSourceSupplier;
        this.resultSetExtractorSupplier = resultSetExtractorSupplier;
        this.readDelay = readDelay;
        this.readWindow = readWindow;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.dataSource = dataSourceSupplier.get();
        this.resultSetExtractor = resultSetExtractorSupplier.get();
    }

    @Override
    public void run(SourceContext<List<MySrcEvent>> ctx) throws Exception {
        isRunning = true;
        Instant beginningInstant = Instant.ofEpochMilli(beginningTimestamp);
        while (isRunning) {
            Instant now = Instant.now();
            if (Duration.between(beginningInstant, now).compareTo(readDelay) > 0) {
                long endingTimestamp = beginningInstant.plus(readWindow).toEpochMilli();
                List<MySrcEvent> events;
                //noinspection SqlResolve
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT ID, TIMESTAMP FROM MY_SOURCE_EVENT WHERE TIMESTAMP >= ? AND TIMESTAMP < ?")) {
                    statement.setLong(1, beginningInstant.toEpochMilli());
                    statement.setLong(2, endingTimestamp);
                    ResultSet resultSet = statement.executeQuery();
                    events = Optional.ofNullable(resultSetExtractor.extractData(resultSet)).orElse(Collections.emptyList());
                    connection.commit();
                }

                synchronized (ctx.getCheckpointLock()) {
                    ctx.collectWithTimestamp(events, beginningInstant.toEpochMilli());
                    beginningTimestamp = endingTimestamp;
                }
            }
            safeWait(ctx);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    private void safeWait(SourceContext<?> ctx) {
        try {
            ctx.markAsTemporarilyIdle();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // swallow interruption unless source is canceled
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        beginningTimestampState.clear();
        beginningTimestampState.add(beginningTimestamp);
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        ListStateDescriptor<Long> timestampStartDescriptor = new ListStateDescriptor<>("db-timestamp-start", Long.class);
        this.beginningTimestampState = context.getOperatorStateStore().getListState(timestampStartDescriptor);
        if (context.isRestored()) {
            for (Long timestamp : beginningTimestampState.get()) {
                this.beginningTimestamp = timestamp;
                break;
            }
        }
    }
}
