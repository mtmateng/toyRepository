package com.lifeStory.selfImplInterface;

import com.lifeStory.DataSourceStore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StudentRepositoryImpl implements StudentInterface {

    private final DataSource dataSource;

    public StudentRepositoryImpl() {

        this.dataSource = DataSourceStore.getDataSource("student");

    }

    public Map<String, Object> getMapById(Integer id) {

        String sql = "select * from student where id = " + id.toString();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            resultSet.next();
            Map<String, Object> ret = new HashMap<>();
            ret.put("id", resultSet.getInt("id"));
            ret.put("name", resultSet.getString("name"));
            ret.put("gender", resultSet.getString("gender"));
            Date date = resultSet.getDate("birthday");
            LocalDate localDate = LocalDate.of(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
            ret.put("birthday", localDate);
            return ret;

        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行sql:%s失败", sql), e);
        }

    }

}
