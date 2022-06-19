package com.github.cuzfrog.task.jdbc;

import com.github.cuzfrog.task.domain.MySrcEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
final class SimpleResultSetExtractor implements ResultSetExtractor<List<MySrcEvent>>, Serializable {
    @Override
    public List<MySrcEvent> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<MySrcEvent> events = new ArrayList<>();
        while (rs.next()) {
            MySrcEvent event = new MySrcEvent();
            event.setId(rs.getLong(1));
            event.setTimestamp(rs.getLong(2));
            events.add(event);
        }
        return events;
    }
}
