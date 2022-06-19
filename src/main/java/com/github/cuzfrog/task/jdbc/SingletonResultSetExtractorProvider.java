package com.github.cuzfrog.task.jdbc;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

@Service("resultSetExtractorSupplier")
final class SingletonResultSetExtractorProvider<T> implements Supplier<ResultSetExtractor<List<T>>>, Serializable {
    private volatile transient ResultSetExtractor<List<T>> extractor;
    @Override
    public synchronized ResultSetExtractor<List<T>> get() {
        if (extractor == null) {
            extractor = new RowMapperResultSetExtractor<T>(new DataClassRowMapper<>());
        }
        return extractor;
    }
}
