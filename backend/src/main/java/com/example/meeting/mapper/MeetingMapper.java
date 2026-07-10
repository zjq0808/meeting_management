package com.example.meeting.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingMapper {
    @Insert("insert into t_meeting_main(meeting_date, meeting_time, period_no, location, leaders, content, status, creater, created_time, updated_time) " +
            "values(#{meetingDate}, #{meetingTime}, #{periodNo}, #{location}, #{leaders}, #{content}, 'DRAFT', #{createdBy}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMeeting(Map<String, Object> meeting);

    @Update("update t_meeting_main set meeting_date=#{meetingDate}, meeting_time=#{meetingTime}, period_no=#{periodNo}, location=#{location}, leaders=#{leaders}, content=#{content}, updated_time=now() where id=#{id}")
    int updateMeeting(Map<String, Object> meeting);

    @Select("select id, meeting_date as meetingDate, meeting_time as meetingTime, period_no as periodNo, location, leaders, content, status, creater as createdBy, created_time as createdAt, updated_time as updatedAt from t_meeting_main where id = #{id}")
    Map<String, Object> findMeeting(@Param("id") Long id);

    @Select("select id, meeting_date as meetingDate, meeting_time as meetingTime, period_no as periodNo, location, leaders, content, status, creater as createdBy, created_time as createdAt, updated_time as updatedAt, " +
            "(select count(1) from t_meeting_topic tt where tt.meeting_id = t_meeting_main.id) as topicCount " +
            "from t_meeting_main order by meeting_date desc, id desc")
    List<Map<String, Object>> listMeetings();

    @Select("select distinct m.id, m.meeting_date as meetingDate, m.meeting_time as meetingTime, m.period_no as periodNo, m.location, m.leaders, m.content, m.status, m.creater as createdBy, m.created_time as createdAt, m.updated_time as updatedAt, " +
            "(select count(1) from t_meeting_topic tt where tt.meeting_id = m.id) as topicCount " +
            "from t_meeting_main m " +
            "left join t_meeting_topic t on t.meeting_id = m.id " +
            "left join t_meeting_attendee ca on ca.meeting_id = m.id and ca.user_id = #{userId} and ca.confirm_status = 'CONFIRMED' " +
            "where m.status <> 'DRAFT' and (m.creater = #{userId} or ca.user_id is not null " +
            "or (#{role} = 'SECRETARY' and (t.report_dept_id = #{departmentId} or t.participant_dept_id like concat('%', #{departmentId}, '%')))) " +
            "order by m.meeting_date desc, m.id desc")
    List<Map<String, Object>> listMeetingsForUser(@Param("userId") String userId, @Param("departmentId") String departmentId, @Param("role") String role);

    @Update("update t_meeting_main set status = #{status}, updated_time = now() where id = #{id}")
    int updateMeetingStatus(@Param("id") Long id, @Param("status") String status);

    @Insert("insert into t_meeting_topic(meeting_id, topic_type, title, report_dept_id, report_dept_name, participant_dept_id, participant_dept_name, summary, sort_no, status, report_dept_signed, created_at, updated_at) " +
            "values(#{meetingId}, #{topicType}, #{title}, #{reportDepartmentId}, #{reportDepartmentName}, #{participantDeptId}, #{participantDepartments}, #{summary}, #{sortNo}, 'WAITING', 0, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTopic(Map<String, Object> topic);

    @Update("update t_meeting_topic set topic_type=#{topicType}, title=#{title}, report_dept_id=#{reportDepartmentId}, report_dept_name=#{reportDepartmentName}, participant_dept_id=#{participantDeptId}, participant_dept_name=#{participantDepartments}, summary=#{summary}, sort_no=#{sortNo}, updated_at=now() where id=#{id}")
    int updateTopic(Map<String, Object> topic);

    @Select("select id, meeting_id as meetingId, topic_type as topicType, title, report_dept_id as reportDepartmentId, report_dept_name as reportDepartmentName, " +
            "participant_dept_id as participantDeptId, participant_dept_name as participantDepartments, summary, sort_no as sortNo, status, start_time as startTime, end_time as endTime, actual_minutes as actualMinutes, conclusion, report_dept_signed as reportDepartmentSigned, created_at as createdAt, updated_at as updatedAt " +
            "from t_meeting_topic where id = #{id}")
    Map<String, Object> findTopic(@Param("id") Long id);

    @Select("select id, meeting_id as meetingId, topic_type as topicType, title, report_dept_id as reportDepartmentId, report_dept_name as reportDepartmentName, " +
            "participant_dept_id as participantDeptId, participant_dept_name as participantDepartments, summary, sort_no as sortNo, status, start_time as startTime, end_time as endTime, actual_minutes as actualMinutes, conclusion, report_dept_signed as reportDepartmentSigned, created_at as createdAt, updated_at as updatedAt " +
            "from t_meeting_topic where meeting_id = #{meetingId} order by sort_no, id")
    List<Map<String, Object>> listTopics(@Param("meetingId") Long meetingId);

    @Delete({"<script>",
            "delete from t_meeting_topic where meeting_id = #{meetingId}",
            "<if test='ids != null and ids.size > 0'>",
            "and id not in",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</if>",
            "</script>"})
    int deleteTopicsNotIn(@Param("meetingId") Long meetingId, @Param("ids") List<Long> ids);

    @Update("update t_meeting_topic set status = #{status}, start_time = #{startTime}, end_time = #{endTime}, actual_minutes = #{actualMinutes}, updated_at = now() where id = #{id}")
    int updateTopicProgress(@Param("id") Long id,
                            @Param("status") String status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime,
                            @Param("actualMinutes") Integer actualMinutes);

    @Update("update t_meeting_topic set conclusion = #{conclusion}, actual_minutes = coalesce(#{actualMinutes}, actual_minutes), updated_at = now() where id = #{id}")
    int updateConclusion(@Param("id") Long id, @Param("conclusion") String conclusion, @Param("actualMinutes") Integer actualMinutes);

    @Update("update t_meeting_topic set report_dept_signed = 1, updated_at = now() where id = #{id}")
    int markReportDepartmentSigned(@Param("id") Long id);

    @Select("select id, meeting_id as meetingId, topic_type as topicType, title, report_dept_id as reportDepartmentId, report_dept_name as reportDepartmentName, " +
            "participant_dept_id as participantDeptId, participant_dept_name as participantDepartments, summary, sort_no as sortNo, status, start_time as startTime, end_time as endTime, actual_minutes as actualMinutes, conclusion, report_dept_signed as reportDepartmentSigned, created_at as createdAt, updated_at as updatedAt " +
            "from t_meeting_topic where meeting_id = #{meetingId} and status = 'RUNNING' limit 1")
    Map<String, Object> findRunningTopic(@Param("meetingId") Long meetingId);

    @Select("select id, meeting_id as meetingId, topic_type as topicType, title, report_dept_id as reportDepartmentId, report_dept_name as reportDepartmentName, " +
            "participant_dept_id as participantDeptId, participant_dept_name as participantDepartments, summary, sort_no as sortNo, status, start_time as startTime, end_time as endTime, actual_minutes as actualMinutes, conclusion, report_dept_signed as reportDepartmentSigned, created_at as createdAt, updated_at as updatedAt " +
            "from t_meeting_topic where meeting_id = #{meetingId} and (report_dept_id = #{departmentId} or participant_dept_id like concat('%', #{departmentId}, '%')) order by sort_no")
    List<Map<String, Object>> listSecretaryTasks(@Param("meetingId") Long meetingId, @Param("departmentId") String departmentId);

    @Select("select t.id, t.meeting_id as meetingId, t.topic_type as topicType, t.title, t.report_dept_id as reportDepartmentId, t.report_dept_name as reportDepartmentName, " +
            "t.participant_dept_id as participantDeptId, t.participant_dept_name as participantDepartments, t.summary, t.sort_no as sortNo, t.status, t.start_time as startTime, t.end_time as endTime, t.actual_minutes as actualMinutes, t.conclusion, t.report_dept_signed as reportDepartmentSigned, t.created_at as createdAt, t.updated_at as updatedAt " +
            "from t_meeting_topic t where t.meeting_id = #{meetingId} and exists(select 1 from t_meeting_attendee a where a.topic_id=t.id and a.user_id=#{userId} and a.attendee_type='LEADER' and a.confirm_status='CONFIRMED') order by t.sort_no")
    List<Map<String, Object>> listLeaderTasks(@Param("meetingId") Long meetingId, @Param("userId") String userId);

    @Insert("insert into t_meeting_attendee(meeting_id, topic_id, user_id, user_name, attendee_type, selected_by, created_at, sign_status, confirm_status) " +
            "values(#{meetingId}, #{topicId}, #{userId}, #{userName}, #{attendeeType}, #{selectedBy}, now(), 'UNSIGNED', 'PENDING')")
    int insertAttendee(@Param("meetingId") Long meetingId,
                       @Param("topicId") Long topicId,
                       @Param("userId") String userId,
                       @Param("userName") String userName,
                       @Param("attendeeType") String attendeeType,
                       @Param("selectedBy") String selectedBy);

    @Delete("delete from t_meeting_attendee where topic_id = #{topicId} and attendee_type = #{attendeeType} and selected_by = #{selectedBy}")
    int deleteAttendeesBySubmitter(@Param("topicId") Long topicId, @Param("attendeeType") String attendeeType, @Param("selectedBy") String selectedBy);

    @Select("select count(1) from t_meeting_attendee where topic_id = #{topicId}")
    int countAttendees(@Param("topicId") Long topicId);

    @Select("select a.*, a.user_id as employee_no, a.user_name as username, a.user_name as real_name, u.f_dept_id as department_id, d.f_dept_nm as department_name " +
            "from t_meeting_attendee a left join t_wxdept_users u on u.f_user_id = a.user_id left join t_wxdept d on d.f_dept_id = u.f_dept_id " +
            "where a.topic_id = #{topicId} order by a.attendee_type, a.user_name")
    List<Map<String, Object>> listTopicAttendees(@Param("topicId") Long topicId);

    @Select("select distinct a.user_id as id, a.user_id as user_id, a.user_id as employee_no, a.user_name as username, a.user_name as real_name, " +
            "u.f_dept_id as department_id, d.f_dept_nm as department_name, u.f_phone as mobile " +
            "from t_meeting_attendee a left join t_wxdept_users u on u.f_user_id = a.user_id left join t_wxdept d on d.f_dept_id = u.f_dept_id " +
            "where a.meeting_id = #{meetingId} order by d.f_dept_nm, a.user_name")
    List<Map<String, Object>> listMeetingAttendees(@Param("meetingId") Long meetingId);

    @Select("select a.*, a.user_id as employee_no, a.user_name as username, a.user_name as real_name, " +
            "u.f_dept_id as department_id, d.f_dept_nm as department_name, t.title as topic_title, t.sort_no as sort_no " +
            "from t_meeting_attendee a " +
            "left join t_wxdept_users u on u.f_user_id = a.user_id " +
            "left join t_wxdept d on d.f_dept_id = u.f_dept_id " +
            "join t_meeting_topic t on t.id = a.topic_id " +
            "where a.meeting_id = #{meetingId} and a.selected_by = #{selectedBy} and a.confirm_status = 'PENDING' " +
            "order by t.sort_no, a.attendee_type, a.user_name")
    List<Map<String, Object>> listAttendeesSelectedBy(@Param("meetingId") Long meetingId, @Param("selectedBy") String selectedBy);

    @Select("select count(1) from t_meeting_attendee where topic_id = #{topicId} and user_id = #{userId}")
    int isTopicAttendee(@Param("topicId") Long topicId, @Param("userId") String userId);

    @Select("select count(1) from t_meeting_attendee where topic_id = #{topicId} and user_id = #{userId} and attendee_type = #{attendeeType} and confirm_status = 'CONFIRMED'")
    int isTopicAttendeeByType(@Param("topicId") Long topicId, @Param("userId") String userId, @Param("attendeeType") String attendeeType);

    @Select("select a.* from t_meeting_attendee a where a.meeting_id=#{meetingId} and a.user_id=#{userId} and a.sign_status='UNSIGNED' and a.confirm_status='CONFIRMED' order by a.topic_id")
    List<Map<String, Object>> listUnsignedAttendeesForUser(@Param("meetingId") Long meetingId, @Param("userId") String userId);

    @Select("select a.id as attendeeId, a.meeting_id as meetingId, a.topic_id as topicId, a.user_id as userId, a.user_name as userName, " +
            "a.attendee_type as attendeeType, a.sign_status as signStatus, a.signed_at as signedAt, " +
            "t.id, t.topic_type as topicType, t.title, t.report_dept_id as reportDepartmentId, t.report_dept_name as reportDepartmentName, " +
            "t.participant_dept_id as participantDeptId, t.participant_dept_name as participantDepartments, t.summary, t.sort_no as sortNo, " +
            "t.status, t.start_time as startTime, t.end_time as endTime, t.actual_minutes as actualMinutes, t.conclusion, t.report_dept_signed as reportDepartmentSigned " +
            "from t_meeting_attendee a join t_meeting_topic t on t.id = a.topic_id " +
            "where a.meeting_id=#{meetingId} and a.user_id=#{userId} and a.confirm_status='CONFIRMED' " +
            "order by t.sort_no, t.id")
    List<Map<String, Object>> listSignTopicsForUser(@Param("meetingId") Long meetingId, @Param("userId") String userId);

    @Update("update t_meeting_attendee set confirm_status='CONFIRMED', confirmed_by=#{confirmedBy}, confirmed_at=now() where id=#{id} and confirm_status='PENDING'")
    int markAttendeeConfirmed(@Param("id") Long id, @Param("confirmedBy") String confirmedBy);

    @Update("update t_meeting_attendee set sign_status='SIGNED', signed_at=now() where id=#{id} and sign_status='UNSIGNED'")
    int markAttendeeSigned(@Param("id") Long id);

    @Select("select a.*, a.user_id as employee_no, a.user_name as real_name, u.f_dept_id as department_id, t.title as topic_title " +
            "from t_meeting_attendee a left join t_wxdept_users u on u.f_user_id=a.user_id join t_meeting_topic t on t.id=a.topic_id " +
            "where a.meeting_id=#{meetingId} and a.sign_status='SIGNED' order by a.signed_at desc")
    List<Map<String, Object>> listSignIns(@Param("meetingId") Long meetingId);

    @Insert("insert into t_meeting_notification(meeting_id, topic_id, title, msg_body, tousers, send_time, job_id, created_at) " +
            "values(#{meetingId}, #{topicId}, #{title}, #{content}, #{tousers}, now(), #{jobId}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(Map<String, Object> notice);

    @Select("select n.* from t_meeting_notification n where n.tousers like concat('%', #{userId}, '%') order by n.created_at desc")
    List<Map<String, Object>> listNotifications(@Param("userId") String userId);

    @Insert("insert into t_meeting_uploaded_file(meeting_id, file_name, file_size, parser_status, parser_message, created_at) values(#{meetingId}, #{fileName}, #{fileSize}, #{parserStatus}, #{parserMessage}, now())")
    int insertUploadedFile(Map<String, Object> file);
}
