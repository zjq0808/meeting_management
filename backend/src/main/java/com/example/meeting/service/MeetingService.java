package com.example.meeting.service;

import com.example.meeting.mapper.MeetingMapper;
import com.example.meeting.mapper.UserMapper;
import com.example.meeting.util.BusinessException;
import com.example.meeting.util.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MeetingService {
    private static final String SHARE_NOTICE_TITLE = "\u4f1a\u8bae\u8bae\u9898\u5206\u4eab\u901a\u77e5";

    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;
    private final TopicFileParser topicFileParser;
    private final NotificationSender notificationSender;
    private final int signTokenTtlMinutes;
    private final int maxAttendeesPerTopic;
    private final String groupCode;
    private final Map<String, SignToken> signTokens = new ConcurrentHashMap<String, SignToken>();

    public MeetingService(MeetingMapper meetingMapper,
                          UserMapper userMapper,
                          TopicFileParser topicFileParser,
                          NotificationSender notificationSender,
                          @Value("${meeting.sign-token-ttl-minutes:240}") int signTokenTtlMinutes,
                          @Value("${meeting.max-attendees-per-topic:60}") int maxAttendeesPerTopic,
                          @Value("${meeting.group-code:DEFAULT}") String groupCode) {
        this.meetingMapper = meetingMapper;
        this.userMapper = userMapper;
        this.topicFileParser = topicFileParser;
        this.notificationSender = notificationSender;
        this.signTokenTtlMinutes = signTokenTtlMinutes;
        this.maxAttendeesPerTopic = maxAttendeesPerTopic;
        this.groupCode = groupCode;
    }

    @Transactional
    public Map<String, Object> createMeeting(Map<String, Object> request, Map<String, Object> currentUser) {
        assertAdmin(currentUser);
        request.put("createdBy", userId(currentUser));
        meetingMapper.insertMeeting(request);
        Long meetingId = Maps.longValue(request, "id");
        saveTopics(meetingId, topicsFrom(request));
        return detail(meetingId);
    }

    @Transactional
    public Map<String, Object> updateMeeting(Long meetingId, Map<String, Object> request, Map<String, Object> currentUser) {
        assertAdmin(currentUser);
        request.put("id", meetingId);
        meetingMapper.updateMeeting(request);
        int sort = 1;
        List<Long> keptTopicIds = new ArrayList<Long>();
        for (Map<String, Object> topic : topicsFrom(request)) {
            topic.put("meetingId", meetingId);
            prepareTopicForSave(topic, sort);
            if (Maps.longValue(topic, "id") == null) {
                meetingMapper.insertTopic(topic);
            } else {
                meetingMapper.updateTopic(topic);
            }
            Long savedTopicId = Maps.longValue(topic, "id");
            if (savedTopicId != null) {
                keptTopicIds.add(savedTopicId);
            }
            sort++;
        }
        meetingMapper.deleteTopicsNotIn(meetingId, keptTopicIds);
        return detail(meetingId);
    }

    public List<Map<String, Object>> listMeetings(Map<String, Object> currentUser) {
        List<Map<String, Object>> meetings;
        if ("ADMIN".equals(Maps.stringValue(currentUser, "role"))) {
            meetings = meetingMapper.listMeetings();
        } else {
            meetings = meetingMapper.listMeetingsForUser(userId(currentUser),
                    Maps.stringValue(currentUser, "departmentId", "department_id"),
                    Maps.stringValue(currentUser, "role"));
        }
        for (Map<String, Object> meeting : meetings) {
            normalizeMeetingMap(meeting);
        }
        return meetings;
    }

    public Map<String, Object> detail(Long meetingId) {
        Map<String, Object> meeting = normalizeMeetingMap(requireMeeting(meetingId));
        List<Map<String, Object>> topics = meetingMapper.listTopics(meetingId);
        for (Map<String, Object> topic : topics) {
            normalizeTopicMap(topic);
            Long topicId = Maps.longValue(topic, "id");
            topic.put("participantDepartmentIds", parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id")));
            topic.put("attendees", meetingMapper.listTopicAttendees(topicId));
        }
        meeting.put("topics", topics);
        meeting.put("signIns", meetingMapper.listSignIns(meetingId));
        meeting.put("attendees", meetingMapper.listMeetingAttendees(meetingId));
        meeting.put("topicSignStats", topicSignStats(meetingId, topics));
        return meeting;
    }

    public Map<String, Object> detail(Long meetingId, Map<String, Object> currentUser) {
        Map<String, Object> meeting = detail(meetingId);
        if (!"ADMIN".equals(Maps.stringValue(currentUser, "role")) && "DRAFT".equals(Maps.stringValue(meeting, "status"))) {
            throw new BusinessException("会议尚未发布，当前角色不可查看");
        }
        applyTopicPermissions(meeting, currentUser);
        return meeting;
    }

    @Transactional
    public Map<String, Object> importTopics(Long meetingId, MultipartFile file) {
        requireMeeting(meetingId);
        Map<String, Object> upload = new HashMap<String, Object>();
        upload.put("meetingId", meetingId);
        upload.put("fileName", file == null ? "" : file.getOriginalFilename());
        upload.put("fileSize", file == null ? 0 : file.getSize());
        try {
            List<Map<String, Object>> topics = topicFileParser.parse(file, userMapper.listDepartments());
            upload.put("parserStatus", "SUCCESS");
            upload.put("parserMessage", "识别到 " + topics.size() + " 个议题");
            meetingMapper.deleteTopicsNotIn(meetingId, new ArrayList<Long>());
            saveTopics(meetingId, topics);
            meetingMapper.insertUploadedFile(upload);
            List<Map<String, Object>> savedTopics = meetingMapper.listTopics(meetingId);
            for (Map<String, Object> topic : savedTopics) {
                normalizeTopicMap(topic);
            }
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("meetingId", meetingId);
            result.put("fileName", upload.get("fileName"));
            result.put("parserStatus", upload.get("parserStatus"));
            result.put("parserMessage", upload.get("parserMessage"));
            result.put("topicCount", savedTopics.size());
            result.put("topics", savedTopics);
            return result;
        } catch (BusinessException ex) {
            upload.put("parserStatus", "FAILED");
            upload.put("parserMessage", ex.getMessage());
            meetingMapper.insertUploadedFile(upload);
            throw ex;
        }
    }

    public Map<String, Object> parseTopics(MultipartFile file) {
        List<Map<String, Object>> topics = topicFileParser.parse(file, userMapper.listDepartments());
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meetingId", null);
        result.put("fileName", file == null ? "" : file.getOriginalFilename());
        result.put("parserStatus", "SUCCESS");
        result.put("parserMessage", "识别到 " + topics.size() + " 个议题");
        result.put("topicCount", topics.size());
        result.put("topics", topics);
        return result;
    }

    @Transactional
    public Map<String, Object> publish(Long meetingId) {
        Map<String, Object> meeting = requireMeeting(meetingId);
        List<Map<String, Object>> topics = meetingMapper.listTopics(meetingId);
        if (topics.isEmpty()) {
            throw new BusinessException("会议至少需要一个议题才能发布");
        }
        meetingMapper.updateMeetingStatus(meetingId, "PUBLISHED");
        Set<String> missingSecretaryDepartments = new LinkedHashSet<String>();
        for (Map<String, Object> topic : topics) {
            Set<String> notifiedDepartments = new LinkedHashSet<String>();
            String reportDepartmentId = Maps.stringValue(topic, "reportDepartmentId", "report_department_id");
            notifiedDepartments.add(reportDepartmentId);
            if (!notifySecretary(meeting, topic, reportDepartmentId)) {
                missingSecretaryDepartments.add(reportDepartmentId);
            }
            for (String departmentId : parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id"))) {
                if (notifiedDepartments.contains(departmentId)) {
                    continue;
                }
                notifiedDepartments.add(departmentId);
                if (!notifySecretary(meeting, topic, departmentId)) {
                    missingSecretaryDepartments.add(departmentId);
                }
            }
        }
        missingSecretaryDepartments.remove(null);
        if (!missingSecretaryDepartments.isEmpty()) {
            notifyAdmins(meetingId, "部分部门未匹配到秘书，请手动补充联系人：" + missingSecretaryDepartments);
        }
        return detail(meetingId);
    }

    public List<Map<String, Object>> selectionTasks(Long meetingId, Map<String, Object> currentUser) {
        String role = Maps.stringValue(currentUser, "role");
        if ("SECRETARY".equals(role)) {
            return meetingMapper.listSecretaryTasks(meetingId, Maps.stringValue(currentUser, "departmentId", "department_id"));
        }
        if ("LEADER".equals(role)) {
            return meetingMapper.listLeaderTasks(meetingId, userId(currentUser));
        }
        List<Map<String, Object>> sharedTasks = meetingMapper.listSharedTasks(meetingId, userId(currentUser));
        if (!sharedTasks.isEmpty()) {
            return sharedTasks;
        }
        return meetingMapper.listTopics(meetingId);
    }

    @Transactional
    public Map<String, Object> submitAttendees(Long topicId, Map<String, Object> request, Map<String, Object> currentUser) {
        Map<String, Object> topic = requireTopic(topicId);
        List<String> userIds = requestUserIds(request.get("userIds"));
        if (userIds.isEmpty()) {
            throw new BusinessException("请至少勾选一名参会人员");
        }
        String attendeeType = normalizeAttendeeType(Maps.stringValue(request, "attendeeType"), currentUser);
        assertCanSubmitAttendees(topic, attendeeType, currentUser);
        String submitter = userId(currentUser);
        meetingMapper.deleteAttendeesBySubmitter(topicId, attendeeType, submitter);
        Long meetingId = Maps.longValue(topic, "meetingId", "meeting_id");
        List<Map<String, Object>> usersToInsert = new ArrayList<Map<String, Object>>();
        for (String selectedUserId : userIds) {
            Map<String, Object> selectedUser = userMapper.findById(selectedUserId, groupCode);
            if (selectedUser == null) {
                throw new BusinessException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u672a\u540c\u6b65\uff1a" + selectedUserId);
            }
            assertAttendeeMatchesDepartment(topic, attendeeType, selectedUser);
            if (meetingMapper.isTopicAttendeeByType(topicId, selectedUserId, attendeeType) > 0) {
                continue;
            }
            usersToInsert.add(selectedUser);
        }
        if (meetingMapper.countAttendees(topicId) + usersToInsert.size() > maxAttendeesPerTopic) {
            throw new BusinessException("\u8d85\u8fc7\u6700\u5927\u53c2\u4f1a\u4eba\u6570\u9650\u5236\uff1a" + maxAttendeesPerTopic);
        }
        for (Map<String, Object> selectedUser : usersToInsert) {
            String selectedUserId = Maps.stringValue(selectedUser, "id", "userId", "user_id");
            meetingMapper.insertAttendee(meetingId, topicId, selectedUserId,
                    Maps.stringValue(selectedUser, "username", "realName"),
                    attendeeType, submitter,
                    "ADMIN".equals(Maps.stringValue(currentUser, "role")) ? "ADMIN" : "DEPARTMENT",
                    "ADMIN".equals(Maps.stringValue(currentUser, "role")) ? null : Maps.stringValue(currentUser, "departmentId", "department_id"),
                    "PENDING", null, null);
        }
        notifySubmitSuccess(topic, submitter);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("topic", meetingMapper.findTopic(topicId));
        result.put("attendees", meetingMapper.listTopicAttendees(topicId));
        return result;
    }

    @Transactional
    public Map<String, Object> createSignQrCode(Long meetingId) {
        requireMeeting(meetingId);
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(signTokenTtlMinutes);
        signTokens.put(token, new SignToken(meetingId, expiresAt));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meetingId", meetingId);
        result.put("token", token);
        result.put("expiresAt", expiresAt);
        result.put("url", "/mobile/sign-in?token=" + token);
        return result;
    }
    public Map<String, Object> signInPreview(String token, Map<String, Object> currentUser) {
        SignToken signToken = requireValidSignToken(token);
        Map<String, Object> meeting = normalizeMeetingMap(requireMeeting(signToken.meetingId));
        List<Map<String, Object>> topics = meetingMapper.listSignTopicsForUser(signToken.meetingId, userId(currentUser));
        boolean canSign = false;
        for (Map<String, Object> topic : topics) {
            normalizeTopicMap(topic);
            topic.put("signed", "SIGNED".equals(Maps.stringValue(topic, "signStatus", "sign_status")));
            if (!Boolean.TRUE.equals(topic.get("signed"))) {
                canSign = true;
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meeting", meeting);
        result.put("topics", topics);
        result.put("canSign", canSign);
        result.put("message", topics.isEmpty() ? "\u5f53\u524d\u767b\u5f55\u4eba\u4e0d\u5728\u672c\u6b21\u4f1a\u8bae\u53c2\u4f1a\u540d\u5355\u4e2d" : "");
        result.put("expiresAt", signToken.expiresAt);
        return result;
    }

    @Transactional
    public Map<String, Object> signIn(Map<String, Object> request, Map<String, Object> currentUser) {
        String token = Maps.stringValue(request, "token");
        SignToken signToken = requireValidSignToken(token);
        String currentUserId = userId(currentUser);
        List<Map<String, Object>> unsigned = meetingMapper.listUnsignedAttendeesForUser(signToken.meetingId, currentUserId);
        if (unsigned.isEmpty()) {
            throw new BusinessException("\u8eab\u4efd\u4e0d\u5728\u672c\u6b21\u4f1a\u8bae\u53c2\u4f1a\u540d\u5355\u4e2d\u6216\u5df2\u5b8c\u6210\u7b7e\u5230");
        }
        List<Map<String, Object>> signedTopics = new ArrayList<Map<String, Object>>();
        String currentDepartmentId = Maps.stringValue(currentUser, "departmentId", "department_id");
        for (Map<String, Object> attendee : unsigned) {
            if (meetingMapper.markAttendeeSigned(Maps.longValue(attendee, "id")) > 0) {
                Map<String, Object> topic = meetingMapper.findTopic(Maps.longValue(attendee, "topicId", "topic_id"));
                if (topic != null) {
                    if (currentDepartmentId != null && currentDepartmentId.equals(Maps.stringValue(topic, "reportDepartmentId", "report_department_id"))) {
                        meetingMapper.markReportDepartmentSigned(Maps.longValue(topic, "id"));
                    }
                    signedTopics.add(topic);
                }
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("message", "\u7b7e\u5230\u6210\u529f");
        result.put("topics", signedTopics);
        return result;
    }

    private SignToken requireValidSignToken(String token) {
        if (token == null || token.trim().length() == 0) {
            throw new BusinessException("\u4e8c\u7ef4\u7801\u65e0\u6548");
        }
        SignToken signToken = signTokens.get(token);
        if (signToken == null) {
            throw new BusinessException("\u4e8c\u7ef4\u7801\u65e0\u6548");
        }
        if (signToken.expiresAt.isBefore(LocalDateTime.now())) {
            signTokens.remove(token);
            throw new BusinessException("\u4e8c\u7ef4\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u5237\u65b0");
        }
        return signToken;
    }
    @Transactional
    public Map<String, Object> startTopic(Long topicId) {
        Map<String, Object> topic = requireTopic(topicId);
        Long meetingId = Maps.longValue(topic, "meetingId", "meeting_id");
        if (meetingMapper.findRunningTopic(meetingId) != null) {
            throw new BusinessException("请先结束当前议题");
        }
        meetingMapper.updateMeetingStatus(meetingId, "IN_PROGRESS");
        meetingMapper.updateTopicProgress(topicId, "RUNNING", LocalDateTime.now(), null, null);
        return dashboard(meetingId);
    }

    @Transactional
    public Map<String, Object> endTopic(Long topicId, Map<String, Object> request) {
        Map<String, Object> topic = requireTopic(topicId);
        LocalDateTime startTime = Maps.dateTimeValue(topic, "startTime", "start_time");
        if (startTime == null || !"RUNNING".equals(Maps.stringValue(topic, "status"))) {
            throw new BusinessException("议题尚未开始");
        }
        LocalDateTime endTime = LocalDateTime.now();
        Integer manualMinutes = Maps.intValue(request, "actualMinutes");
        int actualMinutes = manualMinutes == null ? Math.max(1, (int) Duration.between(startTime, endTime).toMinutes()) : manualMinutes;
        meetingMapper.updateTopicProgress(topicId, "FINISHED", startTime, endTime, actualMinutes);
        Long meetingId = Maps.longValue(topic, "meetingId", "meeting_id");
        boolean allFinished = true;
        for (Map<String, Object> item : meetingMapper.listTopics(meetingId)) {
            if (!"FINISHED".equals(Maps.stringValue(item, "status"))) {
                allFinished = false;
                break;
            }
        }
        if (allFinished) {
            meetingMapper.updateMeetingStatus(meetingId, "FINISHED");
        }
        return dashboard(meetingId);
    }

    @Transactional
    public Map<String, Object> saveConclusion(Long topicId, Map<String, Object> request) {
        meetingMapper.updateConclusion(topicId, Maps.stringValue(request, "conclusion"), Maps.intValue(request, "actualMinutes"));
        return meetingMapper.findTopic(topicId);
    }

    @Transactional
    public Map<String, Object> updateTopicDetails(Long topicId, Map<String, Object> request, Map<String, Object> currentUser) {
        assertAdmin(currentUser);
        requireTopic(topicId);
        Map<String, Object> topic = new HashMap<String, Object>();
        topic.put("id", topicId);
        topic.put("title", Maps.stringValue(request, "title"));
        topic.put("topicType", Maps.stringValue(request, "topicType"));
        topic.put("summary", Maps.stringValue(request, "summary"));
        topic.put("conclusion", Maps.stringValue(request, "conclusion"));
        meetingMapper.updateTopicDetails(topic);
        return meetingMapper.findTopic(topicId);
    }

    @Transactional
    public Map<String, Object> notifyTopicPreparation(Long topicId, Map<String, Object> request, Map<String, Object> currentUser) {
        Map<String, Object> topic = requireTopic(topicId);
        Map<String, Object> meeting = requireMeeting(Maps.longValue(topic, "meetingId", "meeting_id"));
        String role = Maps.stringValue(currentUser, "role");
        if (!"ADMIN".equals(role) && !"SECRETARY".equals(role) && !"LEADER".equals(role) && !hasSharedTopicAccess(topic, currentUser)) {
            throw new BusinessException("\u5f53\u524d\u89d2\u8272\u65e0\u6743\u53d1\u9001\u901a\u77e5");
        }
        if ("DRAFT".equals(Maps.stringValue(meeting, "status"))) {
            throw new BusinessException("\u4f1a\u8bae\u5c1a\u672a\u53d1\u5e03\uff0c\u4e0d\u80fd\u901a\u77e5\u53c2\u4f1a\u4eba\u5458");
        }
        assertCanOperateTopic(topic, currentUser);
        List<String> shareUserIds = request == null ? new ArrayList<String>() : requestUserIds(request.get("shareUserIds"));
        if (!shareUserIds.isEmpty()) {
            return shareTopic(topic, meeting, shareUserIds, currentUser);
        }
        List<String> targetUserIds = request == null ? new ArrayList<String>() : requestUserIds(request.get("attendeeUserIds"));
        List<Map<String, Object>> attendees = meetingMapper.listTopicAttendees(topicId);
        List<Map<String, Object>> targets = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> attendee : attendees) {
            String userId = Maps.stringValue(attendee, "userId", "user_id");
            String confirmStatus = Maps.stringValue(attendee, "confirmStatus", "confirm_status");
            if (!"PENDING".equals(confirmStatus)) {
                continue;
            }
            if (!targetUserIds.isEmpty() && !targetUserIds.contains(userId)) {
                continue;
            }
            targets.add(attendee);
        }
        if (targets.isEmpty()) {
            throw new BusinessException("\u6682\u65e0\u53ef\u901a\u77e5\u7684\u53c2\u4f1a\u4eba\u5458");
        }
        for (Map<String, Object> attendee : targets) {
            String userId = Maps.stringValue(attendee, "userId", "user_id");
            String attendeeType = Maps.stringValue(attendee, "attendeeType", "attendee_type");
            Map<String, Object> notice = notice(
                    Maps.longValue(meeting, "id"),
                    topicId,
                    userId,
                    "\u4f1a\u8bae\u5165\u573a\u51c6\u5907\u901a\u77e5",
                    "\u60a8\u5df2\u88ab\u9009\u4e3a\u8bae\u9898\u300a" + Maps.stringValue(topic, "title") + "\u300b\u7684" + ("REPORT".equals(attendeeType) ? "\u6c47\u62a5\u4eba" : "PARTAKE".equals(attendeeType) ? "\u53c2\u4f1a\u4eba" : attendeeType) + "\uff0c\u8bf7\u505a\u597d\u5165\u573a\u51c6\u5907\u3002");
            meetingMapper.insertNotification(notice);
            notificationSender.send(notice);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meetingId", Maps.longValue(meeting, "id"));
        result.put("topicId", topicId);
        result.put("notifiedCount", targets.size());
        result.put("attendees", targets);
        return result;
    }

    private Map<String, Object> shareTopic(Map<String, Object> topic, Map<String, Object> meeting, List<String> shareUserIds, Map<String, Object> currentUser) {
        Long meetingId = Maps.longValue(meeting, "id");
        Long topicId = Maps.longValue(topic, "id");
        String currentDepartmentId = Maps.stringValue(currentUser, "departmentId", "department_id");
        List<Map<String, Object>> targets = new ArrayList<Map<String, Object>>();
        for (String selectedUserId : shareUserIds) {
            Map<String, Object> selectedUser = userMapper.findById(selectedUserId, groupCode);
            if (selectedUser == null) {
                throw new BusinessException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u672a\u540c\u6b65\uff1a" + selectedUserId);
            }
            String selectedDepartmentId = Maps.stringValue(selectedUser, "departmentId", "department_id");
            if (currentDepartmentId != null && currentDepartmentId.length() > 0 && !currentDepartmentId.equals(selectedDepartmentId)) {
                throw new BusinessException("\u53ea\u80fd\u5206\u4eab\u7ed9\u672c\u90e8\u95e8\u4eba\u5458");
            }
            targets.add(selectedUser);
            Map<String, Object> notice = notice(
                    meetingId,
                    topicId,
                    selectedUserId,
                    SHARE_NOTICE_TITLE,
                    "\u60a8\u5df2\u6536\u5230\u8bae\u9898\u300a" + Maps.stringValue(topic, "title") + "\u300b\u7684\u5206\u4eab\u5904\u7406\u6743\u9650\uff0c\u53ef\u534f\u52a9\u9009\u62e9\u6c47\u62a5\u4eba\u548c\u53c2\u4f1a\u4eba\u3002");
            meetingMapper.insertNotification(notice);
            notificationSender.send(notice);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meetingId", meetingId);
        result.put("topicId", topicId);
        result.put("sharedCount", targets.size());
        result.put("attendees", targets);
        return result;
    }

    @Transactional
    public Map<String, Object> confirmAttendees(Long meetingId, Map<String, Object> currentUser) {
        String role = Maps.stringValue(currentUser, "role");
        if (!"SECRETARY".equals(role) && !"LEADER".equals(role)) {
            throw new BusinessException("当前角色无需确认参会人员");
        }
        Map<String, Object> meeting = requireMeeting(meetingId);
        if ("DRAFT".equals(Maps.stringValue(meeting, "status"))) {
            throw new BusinessException("会议尚未发布，不能确认参会人员");
        }
        List<Map<String, Object>> selected = meetingMapper.listAttendeesSelectedBy(meetingId, userId(currentUser));
        if (selected.isEmpty()) {
            throw new BusinessException("暂无可确认的参会人员");
        }
        for (Map<String, Object> attendee : selected) {
            String userId = Maps.stringValue(attendee, "userId", "user_id");
            String topicTitle = Maps.stringValue(attendee, "topicTitle", "topic_title");
            Map<String, Object> notice = notice(meetingId, Maps.longValue(attendee, "topicId", "topic_id"), userId,
                    "会议参会确认通知",
                    "您已被确认参加议题《" + topicTitle + "》，请按会议通知准时参会。");
            meetingMapper.insertNotification(notice);
            notificationSender.send(notice);
            meetingMapper.markAttendeeConfirmed(Maps.longValue(attendee, "id"), userId(currentUser));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("meetingId", meetingId);
        result.put("confirmedCount", selected.size());
        result.put("attendees", selected);
        return result;
    }

    public Map<String, Object> dashboard(Long meetingId) {
        Map<String, Object> detail = detail(meetingId);
        List<Map<String, Object>> topics = meetingMapper.listTopics(meetingId);
        Map<String, Object> running = meetingMapper.findRunningTopic(meetingId);
        int signedCount = meetingMapper.listSignIns(meetingId).size();
        int attendeeCount = 0;
        for (Map<String, Object> topic : topics) {
            attendeeCount += meetingMapper.listTopicAttendees(Maps.longValue(topic, "id")).size();
        }
        detail.put("currentTopic", running);
        detail.put("signedCount", signedCount);
        detail.put("attendeeCount", attendeeCount);
        detail.put("topicCount", topics.size());
        List<Map<String, Object>> topicSignStats = topicSignStats(meetingId, topics);
        detail.put("signedTopicCount", signedTopicCount(topicSignStats));
        detail.put("topicSignStats", topicSignStats);
        return detail;
    }

    private int signedTopicCount(List<Map<String, Object>> topicSignStats) {
        int count = 0;
        for (Map<String, Object> item : topicSignStats) {
            if (Boolean.TRUE.equals(item.get("signed"))) {
                count++;
            }
        }
        return count;
    }

    private Map<String, Object> normalizeMeetingMap(Map<String, Object> meeting) {
        if (meeting == null) {
            return null;
        }
        meeting.put("meetingDate", Maps.stringValue(meeting, "meetingDate", "meeting_date"));
        meeting.put("meetingTime", Maps.stringValue(meeting, "meetingTime", "meeting_time"));
        meeting.put("periodNo", Maps.stringValue(meeting, "periodNo", "period_no"));
        meeting.put("leaders", Maps.stringValue(meeting, "leaders"));
        meeting.put("topicCount", Maps.intValue(meeting, "topicCount", "topic_count"));
        meeting.put("createdBy", Maps.stringValue(meeting, "createdBy", "created_by", "creater"));
        meeting.put("createdAt", Maps.dateTimeValue(meeting, "createdAt", "created_at", "created_time"));
        meeting.put("updatedAt", Maps.dateTimeValue(meeting, "updatedAt", "updated_at", "updated_time"));
        return meeting;
    }

    private Map<String, Object> normalizeTopicMap(Map<String, Object> topic) {
        if (topic == null) {
            return null;
        }
        topic.put("meetingId", Maps.longValue(topic, "meetingId", "meeting_id"));
        topic.put("topicType", Maps.stringValue(topic, "topicType", "topic_type"));
        topic.put("reportDepartmentId", Maps.stringValue(topic, "reportDepartmentId", "report_department_id", "reportDeptId", "report_dept_id"));
        topic.put("reportDepartmentName", Maps.stringValue(topic, "reportDepartmentName", "report_department_name", "reportDeptName", "report_dept_name"));
        topic.put("participantDeptId", Maps.stringValue(topic, "participantDeptId", "participant_dept_id"));
        topic.put("participantDepartments", Maps.stringValue(topic, "participantDepartments", "participant_departments", "participantDeptName", "participant_dept_name"));
        topic.put("sortNo", Maps.intValue(topic, "sortNo", "sort_no"));
        topic.put("startTime", Maps.dateTimeValue(topic, "startTime", "start_time"));
        topic.put("endTime", Maps.dateTimeValue(topic, "endTime", "end_time"));
        topic.put("actualMinutes", Maps.intValue(topic, "actualMinutes", "actual_minutes"));
        topic.put("notice", Maps.stringValue(topic, "notice"));
        topic.put("projectCode", Maps.stringValue(topic, "projectCode", "project_code"));
        topic.put("reportDepartmentSigned", Maps.intValue(topic, "reportDepartmentSigned", "report_department_signed", "reportDeptSigned", "report_dept_signed"));
        return topic;
    }

    private List<Map<String, Object>> topicSignStats(Long meetingId, List<Map<String, Object>> topics) {
        List<Map<String, Object>> signIns = meetingMapper.listSignIns(meetingId);
        List<Map<String, Object>> stats = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> topic : topics) {
            Map<String, Object> item = new HashMap<String, Object>();
            Long topicId = Maps.longValue(topic, "id");
            item.put("id", topicId);
            item.put("sortNo", Maps.intValue(topic, "sortNo"));
            item.put("title", Maps.stringValue(topic, "title"));
            item.put("reportDepartmentName", Maps.stringValue(topic, "reportDepartmentName", "report_department_name"));
            item.put("signed", isReportDepartmentSigned(topic) || hasReportDepartmentSignIn(topicId, Maps.stringValue(topic, "reportDepartmentId", "report_department_id"), signIns));
            stats.add(item);
        }
        return stats;
    }

    private boolean hasReportDepartmentSignIn(Long topicId, String reportDepartmentId, List<Map<String, Object>> signIns) {
        for (Map<String, Object> signIn : signIns) {
            if (topicId != null && topicId.equals(Maps.longValue(signIn, "topicId", "topic_id"))) {
                if (reportDepartmentId == null || reportDepartmentId.equals(Maps.stringValue(signIn, "departmentId", "department_id"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isReportDepartmentSigned(Map<String, Object> topic) {
        Object value = topic.get("reportDepartmentSigned");
        if (value == null) {
            value = topic.get("report_department_signed");
        }
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private boolean notifySecretary(Map<String, Object> meeting, Map<String, Object> topic, String departmentId) {
        if (departmentId == null || departmentId.length() == 0) {
            return false;
        }
        Map<String, Object> secretary = userMapper.findSecretaryByDepartment(departmentId, groupCode);
        if (secretary == null) {
            return false;
        }
        Map<String, Object> notice = notice(Maps.longValue(meeting, "id"), Maps.longValue(topic, "id"), Maps.stringValue(secretary, "id", "userId"),
                "会议参会遴选通知", "请为议题《" + Maps.stringValue(topic, "title") + "》遴选参会人员");
        meetingMapper.insertNotification(notice);
        notificationSender.send(notice);
        return true;
    }

    private void notifyAdmins(Long meetingId, String content) {
        for (Map<String, Object> admin : userMapper.listUsers(null, 1, null, null, groupCode)) {
            Map<String, Object> notice = notice(meetingId, null, Maps.stringValue(admin, "id", "userId"), "联系人缺失提醒", content);
            meetingMapper.insertNotification(notice);
            notificationSender.send(notice);
        }
    }

    private void notifySubmitSuccess(Map<String, Object> topic, String submitter) {
        Map<String, Object> notice = notice(Maps.longValue(topic, "meetingId", "meeting_id"), Maps.longValue(topic, "id"), submitter,
                "参会名单提交成功", "议题《" + Maps.stringValue(topic, "title") + "》参会名单已同步");
        meetingMapper.insertNotification(notice);
        notificationSender.send(notice);
    }

    private Map<String, Object> notice(Long meetingId, Long topicId, String tousers, String title, String content) {
        Map<String, Object> notice = new HashMap<String, Object>();
        notice.put("meetingId", meetingId);
        notice.put("topicId", topicId);
        notice.put("tousers", tousers);
        notice.put("title", title);
        notice.put("content", content);
        notice.put("jobId", UUID.randomUUID().toString().replace("-", ""));
        return notice;
    }

    private void saveTopics(Long meetingId, List<Map<String, Object>> topics) {
        if (topics == null) {
            return;
        }
        int sort = 1;
        for (Map<String, Object> topic : topics) {
            topic.put("meetingId", meetingId);
            prepareTopicForSave(topic, sort);
            meetingMapper.insertTopic(topic);
            sort++;
        }
    }

    private void prepareTopicForSave(Map<String, Object> topic, int defaultSortNo) {
        if (topic.get("sortNo") == null) {
            topic.put("sortNo", defaultSortNo);
        }
        if (topic.get("topicType") == null) {
            topic.put("topicType", "三重一大");
        }
        topic.put("notice", Maps.stringValue(topic, "notice"));
        topic.put("projectCode", Maps.stringValue(topic, "projectCode", "project_code"));
        String reportDepartmentId = Maps.stringValue(topic, "reportDepartmentId", "report_department_id");
        topic.put("reportDepartmentId", reportDepartmentId);
        if (Maps.stringValue(topic, "reportDepartmentName", "report_department_name") == null) {
            topic.put("reportDepartmentName", departmentName(reportDepartmentId));
        }
        List<String> ids = parseDepartmentIds(topic.get("participantDepartmentIds") == null ? topic.get("participantDeptId") : topic.get("participantDepartmentIds"));
        topic.put("participantDeptId", join(ids));
        if (Maps.stringValue(topic, "participantDepartments", "participantDeptName", "participant_dept_name") == null) {
            topic.put("participantDepartments", joinNames(ids));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> topicsFrom(Map<String, Object> request) {
        Object topics = request.get("topics");
        if (topics instanceof List<?>) {
            return (List<Map<String, Object>>) topics;
        }
        return new ArrayList<Map<String, Object>>();
    }

    private List<String> requestUserIds(Object value) {
        List<String> ids = new ArrayList<String>();
        if (!(value instanceof List<?>)) {
            return ids;
        }
        for (Object item : (List<?>) value) {
            if (item != null && String.valueOf(item).trim().length() > 0) {
                ids.add(String.valueOf(item).trim());
            }
        }
        return new ArrayList<String>(new LinkedHashSet<String>(ids));
    }

    private String normalizeAttendeeType(String attendeeType, Map<String, Object> currentUser) {
        String type = attendeeType;
        if (type == null || type.length() == 0) {
            throw new BusinessException("\u8bf7\u6307\u5b9a\u53c2\u4f1a\u7c7b\u578b");
        }
        if ("PARTICIPANT".equals(type)) {
            return "PARTAKE";
        }
        if (Arrays.asList("REPORT", "PARTAKE").contains(type)) {
            return type;
        }
        throw new BusinessException("参会类型不支持：" + type);
    }

    private void assertCanSubmitAttendees(Map<String, Object> topic, String attendeeType, Map<String, Object> currentUser) {
        String role = Maps.stringValue(currentUser, "role");
        if (!"REPORT".equals(attendeeType) && !"PARTAKE".equals(attendeeType)) {
            throw new BusinessException("\u53ea\u80fd\u9009\u62e9\u6c47\u62a5\u4eba\u6216\u53c2\u4f1a\u4eba");
        }
        if ("ADMIN".equals(role)) {
            return;
        }
        if (canSubmitAttendeeType(topic, attendeeType, currentUser)) {
            return;
        }
        throw new BusinessException("\u5f53\u524d\u89d2\u8272\u65e0\u6743\u5904\u7406\u8be5\u8bae\u9898");
    }


    private void assertCanOperateTopic(Map<String, Object> topic, Map<String, Object> currentUser) {
        if (!canOperateTopic(topic, currentUser)) {
            throw new BusinessException("\u5f53\u524d\u89d2\u8272\u65e0\u6743\u5904\u7406\u8be5\u8bae\u9898");
        }
    }

    private boolean canOperateTopic(Map<String, Object> topic, Map<String, Object> currentUser) {
        String role = Maps.stringValue(currentUser, "role");
        if ("ADMIN".equals(role)) {
            return true;
        }
        String departmentId = Maps.stringValue(currentUser, "departmentId", "department_id");
        if ("SECRETARY".equals(role) && canOperateTopicByDepartment(topic, departmentId)) {
            return true;
        }
        return hasSharedTopicAccess(topic, currentUser);
    }

    private boolean canSubmitAttendeeType(Map<String, Object> topic, String attendeeType, Map<String, Object> currentUser) {
        String role = Maps.stringValue(currentUser, "role");
        if (!"SECRETARY".equals(role) && !hasSharedTopicAccess(topic, currentUser)) {
            return false;
        }
        String departmentId = Maps.stringValue(currentUser, "departmentId", "department_id");
        if (departmentId == null || departmentId.length() == 0) {
            return false;
        }
        if ("REPORT".equals(attendeeType)) {
            return parseDepartmentIds(Maps.stringValue(topic, "reportDepartmentId", "report_department_id")).contains(departmentId);
        }
        if ("PARTAKE".equals(attendeeType)) {
            return parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id")).contains(departmentId);
        }
        return false;
    }

    private boolean canOperateTopicByDepartment(Map<String, Object> topic, String departmentId) {
        if (departmentId == null || departmentId.length() == 0) {
            return false;
        }
        List<String> reportDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "reportDepartmentId", "report_department_id"));
        List<String> participantDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id"));
        return reportDepartmentIds.contains(departmentId) || participantDepartmentIds.contains(departmentId);
    }

    private boolean hasSharedTopicAccess(Map<String, Object> topic, Map<String, Object> currentUser) {
        Long meetingId = Maps.longValue(topic, "meetingId", "meeting_id");
        Long topicId = Maps.longValue(topic, "id");
        if (meetingId == null || topicId == null) {
            return false;
        }
        return meetingMapper.countTopicNotifications(meetingId, topicId, userId(currentUser), SHARE_NOTICE_TITLE) > 0;
    }

    @SuppressWarnings("unchecked")
    private void applyTopicPermissions(Map<String, Object> meeting, Map<String, Object> currentUser) {
        Object topicsValue = meeting.get("topics");
        if (!(topicsValue instanceof List<?>)) {
            return;
        }
        String role = Maps.stringValue(currentUser, "role");
        String departmentId = Maps.stringValue(currentUser, "departmentId", "department_id");
        for (Object item : (List<?>) topicsValue) {
            if (!(item instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> topic = (Map<String, Object>) item;
            List<String> reportDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "reportDepartmentId", "report_department_id"));
            List<String> participantDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id"));
            boolean shared = hasSharedTopicAccess(topic, currentUser);
            boolean admin = "ADMIN".equals(role);
            boolean secretary = "SECRETARY".equals(role);
            boolean canReport = admin || ((secretary || shared) && reportDepartmentIds.contains(departmentId));
            boolean canPartake = admin || ((secretary || shared) && participantDepartmentIds.contains(departmentId));
            topic.put("canSelectReporter", canReport);
            topic.put("canSelectParticipant", canPartake);
            topic.put("canShareTopic", !admin && (canReport || canPartake));
            topic.put("sharedAccess", shared);
        }
    }

    private void assertAttendeeMatchesDepartment(Map<String, Object> topic, String attendeeType, Map<String, Object> user) {
        String departmentId = Maps.stringValue(user, "departmentId", "department_id");
        if (departmentId == null || departmentId.length() == 0) {
            return;
        }
        if ("REPORT".equals(attendeeType)) {
            List<String> reportDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "reportDepartmentId", "report_department_id"));
            if (!reportDepartmentIds.contains(departmentId)) {
                throw new BusinessException("\u6c47\u62a5\u4eba\u5fc5\u987b\u5c5e\u4e8e\u6c47\u62a5\u90e8\u95e8");
            }
            return;
        }
        if ("PARTAKE".equals(attendeeType)) {
            List<String> participantDepartmentIds = parseDepartmentIds(Maps.stringValue(topic, "participantDeptId", "participant_dept_id"));
            if (!participantDepartmentIds.contains(departmentId)) {
                throw new BusinessException("\u53c2\u4f1a\u4eba\u5fc5\u987b\u5c5e\u4e8e\u53c2\u4f1a\u90e8\u95e8");
            }
        }
    }

    private List<String> parseDepartmentIds(Object value) {
        List<String> ids = new ArrayList<String>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                addDepartmentId(ids, item);
            }
        } else if (value != null) {
            String[] parts = String.valueOf(value).split(",");
            for (String part : parts) {
                addDepartmentId(ids, part);
            }
        }
        return new ArrayList<String>(new LinkedHashSet<String>(ids));
    }

    private void addDepartmentId(List<String> ids, Object item) {
        if (item != null && String.valueOf(item).trim().length() > 0) {
            ids.add(String.valueOf(item).trim());
        }
    }

    private String departmentName(String departmentId) {
        if (departmentId == null) {
            return null;
        }
        for (Map<String, Object> department : userMapper.listDepartments()) {
            if (departmentId.equals(Maps.stringValue(department, "id", "deptId"))) {
                return Maps.stringValue(department, "name");
            }
        }
        return null;
    }

    private String joinNames(List<String> ids) {
        List<String> names = new ArrayList<String>();
        for (String id : ids) {
            String name = departmentName(id);
            if (name != null) {
                names.add(name);
            }
        }
        return join(names);
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String userId(Map<String, Object> user) {
        return Maps.stringValue(user, "id", "userId", "user_id", "employeeNo", "employee_no");
    }

    private void assertAdmin(Map<String, Object> currentUser) {
        if (!"ADMIN".equals(Maps.stringValue(currentUser, "role"))) {
            throw new BusinessException("仅会议管理员可执行此操作");
        }
    }

    private Map<String, Object> requireMeeting(Long meetingId) {
        Map<String, Object> meeting = meetingMapper.findMeeting(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        return meeting;
    }

    private Map<String, Object> requireTopic(Long topicId) {
        Map<String, Object> topic = meetingMapper.findTopic(topicId);
        if (topic == null) {
            throw new BusinessException("议题不存在");
        }
        return topic;
    }

    private static class SignToken {
        private final Long meetingId;
        private final LocalDateTime expiresAt;

        private SignToken(Long meetingId, LocalDateTime expiresAt) {
            this.meetingId = meetingId;
            this.expiresAt = expiresAt;
        }
    }
}
