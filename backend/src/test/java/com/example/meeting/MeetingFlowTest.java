package com.example.meeting;

import com.example.meeting.service.AuthService;
import com.example.meeting.service.MeetingService;
import com.example.meeting.util.BusinessException;
import com.example.meeting.util.Maps;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MeetingFlowTest {
    @Autowired
    AuthService authService;

    @Autowired
    MeetingService meetingService;

    @Test
    void loginMapsAuthorityRoles() {
        assertThat(Maps.stringValue(user("U0001"), "role")).isEqualTo("ADMIN");
        assertThat(Maps.stringValue(user("U0004"), "role")).isEqualTo("LEADER");
        assertThat(Maps.stringValue(user("U0002"), "role")).isEqualTo("SECRETARY");
        assertThat(Maps.stringValue(user("U0006"), "role")).isEqualTo("PARTICIPANT");
        assertThat(Maps.stringValue(user("U0001"), "employeeNo", "employee_no")).isEqualTo("U0001");
    }

    @Test
    void nonAdminCannotCreateMeeting() {
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                meetingService.createMeeting(meetingPayload(), user("U0002"));
            }
        }).isInstanceOf(BusinessException.class).hasMessageContaining("会议管理员");
    }

    @Test
    void runsMeetingLifecycle() {
        Map<String, Object> admin = user("U0001");
        Map<String, Object> meeting = meetingService.createMeeting(meetingPayload(), admin);
        Long meetingId = Maps.longValue(meeting, "id");
        Long topicId = Maps.longValue(((List<Map<String, Object>>) meeting.get("topics")).get(0), "id");

        assertThat(meetingService.publish(meetingId).get("status")).isEqualTo("PUBLISHED");
        assertThat(meetingService.selectionTasks(meetingId, user("U0002"))).hasSize(1);

        meetingService.submitAttendees(topicId, attendeePayload("U0004", "LEADER"), user("U0002"));
        assertThat(meetingService.confirmAttendees(meetingId, user("U0002")).get("confirmedCount")).isEqualTo(1);
        meetingService.submitAttendees(topicId, attendeePayload("U0006", "PARTICIPANT"), user("U0004"));
        assertThat(meetingService.confirmAttendees(meetingId, user("U0004")).get("confirmedCount")).isEqualTo(1);

        Map<String, Object> qr = meetingService.createSignQrCode(meetingId);
        Map<String, Object> signIn = new HashMap<String, Object>();
        signIn.put("token", qr.get("token"));
        assertThat(meetingService.signIn(signIn, user("U0006")).get("message")).isEqualTo("签到成功");

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                meetingService.signIn(signIn, user("U0007"));
            }
        }).isInstanceOf(BusinessException.class);

        meetingService.startTopic(topicId);
        Map<String, Object> endRequest = new HashMap<String, Object>();
        endRequest.put("actualMinutes", 12);
        Map<String, Object> dashboard = meetingService.endTopic(topicId, endRequest);
        assertThat(dashboard.get("status")).isEqualTo("FINISHED");

        Map<String, Object> conclusion = new HashMap<String, Object>();
        conclusion.put("conclusion", "同意按程序推进");
        assertThat(meetingService.saveConclusion(topicId, conclusion).get("conclusion")).isEqualTo("同意按程序推进");
    }

    @Test
    void importTopicsOnlyParsesAndRecordsUpload() throws Exception {
        Map<String, Object> meeting = meetingService.createMeeting(emptyMeetingPayload(), user("U0001"));
        Long meetingId = Maps.longValue(meeting, "id");
        MockMultipartFile file = new MockMultipartFile("file", "topics.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes());

        Map<String, Object> result = meetingService.importTopics(meetingId, file);

        assertThat(result.get("parserStatus")).isEqualTo("SUCCESS");
        assertThat((List<Map<String, Object>>) result.get("topics")).hasSize(1);
        assertThat((List<Map<String, Object>>) meetingService.detail(meetingId).get("topics")).isEmpty();
    }

    @Test
    void updateMeetingReturnsLatestFieldsAndRemovesDeletedTopics() {
        Map<String, Object> createPayload = meetingPayload();
        List<Map<String, Object>> createTopics = (List<Map<String, Object>>) createPayload.get("topics");
        Map<String, Object> secondTopic = new HashMap<String, Object>();
        secondTopic.put("topicType", "TEST");
        secondTopic.put("title", "topic to delete");
        secondTopic.put("reportDepartmentId", "D003");
        secondTopic.put("participantDepartmentIds", Arrays.asList("D003"));
        secondTopic.put("summary", "delete later");
        secondTopic.put("sortNo", 2);
        createTopics.add(secondTopic);

        Map<String, Object> created = meetingService.createMeeting(createPayload, user("U0001"));
        Long meetingId = Maps.longValue(created, "id");
        List<Map<String, Object>> createdTopics = (List<Map<String, Object>>) created.get("topics");

        Map<String, Object> updatePayload = emptyMeetingPayload();
        updatePayload.put("meetingDate", "2026-08-01");
        updatePayload.put("meetingTime", "10:30");
        updatePayload.put("periodNo", "P-002");
        updatePayload.put("location", "Room 2");
        updatePayload.put("leaders", "Leader A");
        Map<String, Object> keptTopic = new HashMap<String, Object>(createdTopics.get(0));
        keptTopic.put("title", "updated topic");
        keptTopic.put("reportDepartmentId", "D003");
        keptTopic.put("participantDepartmentIds", Arrays.asList("D003"));
        updatePayload.put("topics", Arrays.asList(keptTopic));

        Map<String, Object> updated = meetingService.updateMeeting(meetingId, updatePayload, user("U0001"));
        List<Map<String, Object>> updatedTopics = (List<Map<String, Object>>) updated.get("topics");

        assertThat(updated.get("meetingDate")).isEqualTo("2026-08-01");
        assertThat(updated.get("meetingTime")).isEqualTo("10:30");
        assertThat(updated.get("periodNo")).isEqualTo("P-002");
        assertThat(updated.get("leaders")).isEqualTo("Leader A");
        assertThat(updatedTopics).hasSize(1);
        assertThat(updatedTopics.get(0).get("title")).isEqualTo("updated topic");
        assertThat(updatedTopics.get(0).get("reportDepartmentId")).isEqualTo("D003");
        assertThat(updatedTopics.get(0).get("reportDepartmentName")).isNotNull();
        assertThat((List<String>) updatedTopics.get(0).get("participantDepartmentIds")).containsExactly("D003");
        assertThat(updatedTopics.get(0).get("participantDepartments")).isNotNull();
    }

    @Test
    void validatesUploadAndAttendeeLimits() {
        Map<String, Object> meeting = meetingService.createMeeting(meetingPayload(), user("U0001"));
        Long meetingId = Maps.longValue(meeting, "id");
        Long topicId = Maps.longValue(((List<Map<String, Object>>) meeting.get("topics")).get(0), "id");

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                meetingService.importTopics(meetingId, new MockMultipartFile("file", "topic.txt", "text/plain", "bad".getBytes()));
            }
        }).isInstanceOf(BusinessException.class).hasMessageContaining("Word");

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                meetingService.submitAttendees(topicId, attendeePayload("U0004", "U0005", "U0006", "U0007"), user("U0002"));
            }
        }).isInstanceOf(BusinessException.class).hasMessageContaining("最大参会人数");
    }

    @Test
    void filtersMeetingListByUser() {
        Map<String, Object> admin = user("U0001");
        Map<String, Object> planMeeting = meetingService.createMeeting(meetingPayload("D002", Arrays.asList("D002")), admin);
        Map<String, Object> safeMeeting = meetingService.createMeeting(meetingPayload("D004", Arrays.asList("D004")), admin);
        Long planMeetingId = Maps.longValue(planMeeting, "id");
        Long safeMeetingId = Maps.longValue(safeMeeting, "id");
        Long planTopicId = Maps.longValue(((List<Map<String, Object>>) planMeeting.get("topics")).get(0), "id");

        assertThat(meetingIds(meetingService.listMeetings(admin))).contains(planMeetingId, safeMeetingId);
        assertThat(meetingIds(meetingService.listMeetings(user("U0007")))).doesNotContain(planMeetingId, safeMeetingId);
        assertThat(meetingIds(meetingService.listMeetings(user("U0002")))).doesNotContain(planMeetingId, safeMeetingId);

        meetingService.publish(planMeetingId);
        List<Map<String, Object>> secretaryMeetings = meetingService.listMeetings(user("U0002"));
        assertThat(meetingIds(secretaryMeetings)).contains(planMeetingId).doesNotContain(safeMeetingId);
        assertThat(meetingIds(meetingService.listMeetings(user("U0004")))).doesNotContain(planMeetingId);
        assertThat(meetingIds(meetingService.listMeetings(user("U0006")))).doesNotContain(planMeetingId);

        meetingService.submitAttendees(planTopicId, attendeePayload("U0004", "LEADER"), user("U0002"));
        assertThat(meetingIds(meetingService.listMeetings(user("U0004")))).doesNotContain(planMeetingId);
        meetingService.confirmAttendees(planMeetingId, user("U0002"));
        assertThat(meetingIds(meetingService.listMeetings(user("U0004")))).contains(planMeetingId);
        assertThat(meetingIds(meetingService.listMeetings(user("U0006")))).doesNotContain(planMeetingId);

        meetingService.submitAttendees(planTopicId, attendeePayload("U0006", "PARTICIPANT"), user("U0004"));
        assertThat(meetingIds(meetingService.listMeetings(user("U0006")))).doesNotContain(planMeetingId);
        meetingService.confirmAttendees(planMeetingId, user("U0004"));
        assertThat(meetingIds(meetingService.listMeetings(user("U0006")))).contains(planMeetingId);
        assertThat(secretaryMeetings).filteredOn(item -> Maps.longValue(item, "id").equals(planMeetingId))
                .extracting(item -> Maps.intValue(item, "topicCount", "topic_count"))
                .containsExactly(1);
    }

    @Test
    void returnsDetailAttendeesAndSignDashboardStats() {
        Map<String, Object> meeting = meetingService.createMeeting(meetingPayload(), user("U0001"));
        Long meetingId = Maps.longValue(meeting, "id");
        Long topicId = Maps.longValue(((List<Map<String, Object>>) meeting.get("topics")).get(0), "id");

        meetingService.publish(meetingId);
        meetingService.submitAttendees(topicId, attendeePayload("U0004", "LEADER"), user("U0002"));
        meetingService.confirmAttendees(meetingId, user("U0002"));
        meetingService.submitAttendees(topicId, attendeePayload("U0006", "PARTICIPANT"), user("U0004"));
        meetingService.confirmAttendees(meetingId, user("U0004"));
        Map<String, Object> detail = meetingService.detail(meetingId);
        List<Map<String, Object>> attendees = (List<Map<String, Object>>) detail.get("attendees");
        assertThat(attendees).extracting(item -> Maps.stringValue(item, "employeeNo", "employee_no")).contains("U0006");

        Map<String, Object> qr = meetingService.createSignQrCode(meetingId);
        Map<String, Object> preview = meetingService.signInPreview(Maps.stringValue(qr, "token"), user("U0006"));
        assertThat((List<Map<String, Object>>) preview.get("topics")).hasSize(1);
        assertThat(preview.get("canSign")).isEqualTo(true);

        Map<String, Object> signIn = new HashMap<String, Object>();
        signIn.put("token", qr.get("token"));
        meetingService.signIn(signIn, user("U0006"));

        Map<String, Object> signedPreview = meetingService.signInPreview(Maps.stringValue(qr, "token"), user("U0006"));
        assertThat(signedPreview.get("canSign")).isEqualTo(false);

        Map<String, Object> dashboard = meetingService.dashboard(meetingId);
        assertThat(dashboard.get("topicCount")).isEqualTo(1);
        assertThat(dashboard.get("signedTopicCount")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) dashboard.get("topicSignStats")).hasSize(1);
    }

    private Map<String, Object> user(String userId) {
        Map<String, Object> session = authService.login(userId);
        return (Map<String, Object>) session.get("user");
    }

    private Map<String, Object> emptyMeetingPayload() {
        Map<String, Object> meeting = new HashMap<String, Object>();
        meeting.put("meetingDate", "2026-07-08");
        meeting.put("meetingTime", "09:00");
        meeting.put("periodNo", "2026年第1期");
        meeting.put("location", "第一会议室");
        meeting.put("leaders", "局领导");
        meeting.put("content", "三重一大事项审议");
        meeting.put("topics", new ArrayList<Map<String, Object>>());
        return meeting;
    }

    private Map<String, Object> meetingPayload() {
        return meetingPayload("D002", Arrays.asList("D002", "D003"));
    }

    private Map<String, Object> meetingPayload(String reportDepartmentId, List<String> participantDepartmentIds) {
        Map<String, Object> meeting = emptyMeetingPayload();
        List<Map<String, Object>> topics = new ArrayList<Map<String, Object>>();
        Map<String, Object> topic = new HashMap<String, Object>();
        topic.put("topicType", "重大项目");
        topic.put("title", "技术平台升级事项");
        topic.put("reportDepartmentId", reportDepartmentId);
        topic.put("participantDepartmentIds", participantDepartmentIds);
        topic.put("summary", "审议平台升级计划");
        topic.put("sortNo", 1);
        topics.add(topic);
        meeting.put("topics", topics);
        return meeting;
    }

    private byte[] docxBytes() throws Exception {
        XWPFDocument document = new XWPFDocument();
        addParagraph(document, "议题标题：数据治理项目建设");
        addParagraph(document, "议题类型：重大项目");
        addParagraph(document, "汇报部门：技术规划处");
        addParagraph(document, "参会部门：技术规划处、数据中心");
        addParagraph(document, "议题简述：审议数据治理项目建设方案");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();
        return out.toByteArray();
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText(text);
    }

    private List<Long> meetingIds(List<Map<String, Object>> meetings) {
        List<Long> ids = new ArrayList<Long>();
        for (Map<String, Object> meeting : meetings) {
            ids.add(Maps.longValue(meeting, "id"));
        }
        return ids;
    }

    private Map<String, Object> attendeePayload(String userId, String type) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("userIds", Arrays.asList(userId));
        payload.put("attendeeType", type);
        return payload;
    }

    private Map<String, Object> attendeePayload(String... userIds) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("userIds", Arrays.asList(userIds));
        payload.put("attendeeType", "LEADER");
        return payload;
    }
}
