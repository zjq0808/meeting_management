package com.example.meeting.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingMapper {
    int insertMeeting(Map<String, Object> meeting);

    int updateMeeting(Map<String, Object> meeting);

    Map<String, Object> findMeeting(@Param("id") Long id);

    List<Map<String, Object>> listMeetings();

    List<Map<String, Object>> listMeetingsForUser(@Param("userId") String userId, @Param("departmentId") String departmentId, @Param("role") String role);

    int updateMeetingStatus(@Param("id") Long id, @Param("status") String status);

    int insertTopic(Map<String, Object> topic);

    int updateTopic(Map<String, Object> topic);

    int updateTopicDetails(Map<String, Object> topic);

    int updateConclusion(@Param("id") Long id,
                         @Param("conclusion") String conclusion,
                         @Param("actualMinutes") Integer actualMinutes);

    Map<String, Object> findTopic(@Param("id") Long id);

    List<Map<String, Object>> listTopics(@Param("meetingId") Long meetingId);

    int deleteTopicsNotIn(@Param("meetingId") Long meetingId, @Param("ids") List<Long> ids);

    int updateTopicProgress(@Param("id") Long id,
                            @Param("status") String status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime,
                            @Param("actualMinutes") Integer actualMinutes);

    int markReportDepartmentSigned(@Param("id") Long id);

    Map<String, Object> findRunningTopic(@Param("meetingId") Long meetingId);

    List<Map<String, Object>> listSecretaryTasks(@Param("meetingId") Long meetingId, @Param("departmentId") String departmentId);

    int insertAttendee(@Param("meetingId") Long meetingId,
                       @Param("topicId") Long topicId,
                       @Param("userId") String userId,
                       @Param("userName") String userName,
                       @Param("attendeeType") String attendeeType,
                       @Param("selectedBy") String selectedBy,
                       @Param("selectedSource") String selectedSource,
                       @Param("selectedDeptId") String selectedDeptId,
                       @Param("confirmStatus") String confirmStatus,
                       @Param("confirmedBy") String confirmedBy,
                       @Param("confirmedAt") LocalDateTime confirmedAt);

    int deleteAttendeesBySubmitter(@Param("topicId") Long topicId,
                                   @Param("attendeeType") String attendeeType,
                                   @Param("selectedBy") String selectedBy);

    int deleteAttendee(@Param("id") Long id);

    int countAttendees(@Param("topicId") Long topicId);

    List<Map<String, Object>> listTopicAttendees(@Param("topicId") Long topicId);

    List<Map<String, Object>> listAttendeesSelectedBy(@Param("meetingId") Long meetingId,
                                                      @Param("selectedBy") String selectedBy);

    int isTopicAttendeeByType(@Param("topicId") Long topicId,
                              @Param("userId") String userId,
                              @Param("attendeeType") String attendeeType);

    List<Map<String, Object>> listLeaderTasks(@Param("meetingId") Long meetingId,
                                              @Param("userId") String userId);

    List<Map<String, Object>> listSharedTasks(@Param("meetingId") Long meetingId,
                                              @Param("userId") String userId);

    List<Map<String, Object>> listMeetingAttendees(@Param("meetingId") Long meetingId);

    List<Map<String, Object>> listPendingAttendeesForDepartment(@Param("meetingId") Long meetingId,
                                                                 @Param("departmentId") String departmentId);

    int isTopicAttendee(@Param("topicId") Long topicId, @Param("userId") String userId);

    List<Map<String, Object>> listUnsignedAttendeesForUser(@Param("meetingId") Long meetingId, @Param("userId") String userId);

    List<Map<String, Object>> listSignTopicsForUser(@Param("meetingId") Long meetingId, @Param("userId") String userId);

    int markAttendeeConfirmed(@Param("id") Long id, @Param("confirmedBy") String confirmedBy);

    int markAttendeeSigned(@Param("id") Long id);

    List<Map<String, Object>> listSignIns(@Param("meetingId") Long meetingId);

    int insertNotification(Map<String, Object> notice);

    List<Map<String, Object>> listNotifications(@Param("userId") String userId);

    int countTopicNotifications(@Param("meetingId") Long meetingId,
                                @Param("topicId") Long topicId,
                                @Param("userId") String userId,
                                @Param("title") String title);

    int insertUploadedFile(Map<String, Object> file);
}
