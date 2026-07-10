package com.example.meeting.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    String USER_SELECT = "select u.f_user_id as id, u.f_user_id as user_id, u.f_user_id as employee_no, " +
            "u.f_user_name as username, u.f_user_name as real_name, u.f_dept_id as department_id, " +
            "d.f_dept_nm as department_name, u.f_phone as mobile, u.f_email as email, u.f_position as position, " +
            "coalesce(a.authority_value, 0) as authority_value, " +
            "case coalesce(a.authority_value, 0) when 1 then 'ADMIN' when 2 then 'LEADER' when 3 then 'SECRETARY' else 'PARTICIPANT' end as role ";

    String USER_FROM = "from t_wxdept_users u " +
            "left join t_wxdept d on d.f_dept_id = u.f_dept_id " +
            "left join (select user_id, min(authority) as authority_value from t_meeting_authority where group_code = #{groupCode} group by user_id) a on a.user_id = u.f_user_id ";

    @Select(USER_SELECT + USER_FROM +
            "where (u.f_user_id = #{login} or u.f_user_name = #{login}) and (u.f_enable is null or u.f_enable <> '0') limit 1")
    Map<String, Object> findByLogin(@Param("login") String login, @Param("groupCode") String groupCode);

    @Select(USER_SELECT + USER_FROM + "where u.f_user_id = #{id}")
    Map<String, Object> findById(@Param("id") String id, @Param("groupCode") String groupCode);

    @Select("<script>" +
            USER_SELECT + USER_FROM +
            "where (u.f_enable is null or u.f_enable != '0') " +
            "<if test='departmentId != null and departmentId != \"\"'>and u.f_dept_id = #{departmentId} </if>" +
            "<if test='authority != null'>and a.authority_value = #{authority} </if>" +
            "<if test='participantOnly != null and participantOnly'>and a.authority_value is null </if>" +
            "<if test='keyword != null and keyword != \"\"'>and (u.f_user_name like concat('%', #{keyword}, '%') or u.f_user_id like concat('%', #{keyword}, '%')) </if>" +
            "order by d.f_order_index, u.f_user_name" +
            "</script>")
    List<Map<String, Object>> listUsers(@Param("departmentId") String departmentId,
                                        @Param("authority") Integer authority,
                                        @Param("participantOnly") Boolean participantOnly,
                                        @Param("keyword") String keyword,
                                        @Param("groupCode") String groupCode);

    @Select("select f_dept_id as id, f_dept_id as dept_id, f_dept_nm as name, f_parent_dept_id as parent_id, f_order_index as sort_no from t_wxdept order by f_order_index, f_dept_id")
    List<Map<String, Object>> listDepartments();

    @Select(USER_SELECT + "from t_meeting_authority ma " +
            "join t_wxdept_users u on u.f_user_id = ma.user_id " +
            "left join t_wxdept d on d.f_dept_id = u.f_dept_id " +
            "left join (select user_id, min(authority) as authority_value from t_meeting_authority where group_code = #{groupCode} group by user_id) a on a.user_id = u.f_user_id " +
            "where ma.dept_id = #{departmentId} and ma.authority = 3 and ma.group_code = #{groupCode} order by ma.id limit 1")
    Map<String, Object> findSecretaryByDepartment(@Param("departmentId") String departmentId, @Param("groupCode") String groupCode);
}
