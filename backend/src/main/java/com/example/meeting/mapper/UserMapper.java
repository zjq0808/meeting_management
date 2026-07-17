package com.example.meeting.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    Map<String, Object> findByLogin(@Param("login") String login, @Param("groupCode") String groupCode);

    Map<String, Object> findById(@Param("id") String id, @Param("groupCode") String groupCode);

    List<Map<String, Object>> listUsers(@Param("departmentId") String departmentId,
                                        @Param("authority") Integer authority,
                                        @Param("participantOnly") Boolean participantOnly,
                                        @Param("keyword") String keyword,
                                        @Param("groupCode") String groupCode);

    List<Map<String, Object>> listDepartments();

    Map<String, Object> findSecretaryByDepartment(@Param("departmentId") String departmentId, @Param("groupCode") String groupCode);
}
