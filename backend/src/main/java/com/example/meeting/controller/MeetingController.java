package com.example.meeting.controller;

import com.example.meeting.dto.ApiResponse;
import com.example.meeting.service.MeetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeetingController {
    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping("/meetings")
    public ApiResponse<List<Map<String, Object>>> listMeetings(@RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.listMeetings(currentUser));
    }

    @PostMapping("/meetings")
    public ApiResponse<Map<String, Object>> createMeeting(@RequestBody Map<String, Object> request,
                                                          @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.createMeeting(request, currentUser));
    }

    @PutMapping("/meetings/{id}")
    public ApiResponse<Map<String, Object>> updateMeeting(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> request,
                                                          @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.updateMeeting(id, request, currentUser));
    }

    @GetMapping("/meetings/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                   @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.detail(id, currentUser));
    }

    @GetMapping("/meetings/{id}/detail")
    public ApiResponse<Map<String, Object>> detailAlias(@PathVariable Long id,
                                                        @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.detail(id, currentUser));
    }

    @PostMapping("/meetings/{id}/publish")
    public ApiResponse<Map<String, Object>> publish(@PathVariable Long id) {
        return ApiResponse.ok(meetingService.publish(id));
    }

    @PostMapping("/meetings/{id}/topics/import")
    public ApiResponse<Map<String, Object>> importTopics(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(meetingService.importTopics(id, file));
    }

    @GetMapping("/meetings/{id}/selection-tasks")
    public ApiResponse<List<Map<String, Object>>> selectionTasks(@PathVariable Long id,
                                                                 @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.selectionTasks(id, currentUser));
    }

    @PostMapping("/topics/{topicId}/attendees")
    public ApiResponse<Map<String, Object>> attendees(@PathVariable Long topicId,
                                                      @RequestBody Map<String, Object> request,
                                                      @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.submitAttendees(topicId, request, currentUser));
    }

    @PostMapping("/meetings/{id}/attendees/confirm")
    public ApiResponse<Map<String, Object>> confirmAttendees(@PathVariable Long id,
                                                             @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.confirmAttendees(id, currentUser));
    }

    @PostMapping("/meetings/{id}/sign-qrcode")
    public ApiResponse<Map<String, Object>> signQrCode(@PathVariable Long id) {
        return ApiResponse.ok(meetingService.createSignQrCode(id));
    }

    @GetMapping("/sign-in/preview")
    public ApiResponse<Map<String, Object>> signInPreview(@RequestParam("token") String token,
                                                          @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.signInPreview(token, currentUser));
    }

    @PostMapping("/sign-in")
    public ApiResponse<Map<String, Object>> signIn(@RequestBody Map<String, Object> request,
                                                   @RequestAttribute("currentUser") Map<String, Object> currentUser) {
        return ApiResponse.ok(meetingService.signIn(request, currentUser));
    }

    @PostMapping("/topics/{topicId}/start")
    public ApiResponse<Map<String, Object>> startTopic(@PathVariable Long topicId) {
        return ApiResponse.ok(meetingService.startTopic(topicId));
    }

    @PostMapping("/topics/{topicId}/end")
    public ApiResponse<Map<String, Object>> endTopic(@PathVariable Long topicId, @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.ok(meetingService.endTopic(topicId, request == null ? java.util.Collections.<String, Object>emptyMap() : request));
    }

    @PutMapping("/topics/{topicId}/conclusion")
    public ApiResponse<Map<String, Object>> conclusion(@PathVariable Long topicId, @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(meetingService.saveConclusion(topicId, request));
    }

    @GetMapping("/meetings/{id}/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@PathVariable Long id) {
        return ApiResponse.ok(meetingService.dashboard(id));
    }

    @GetMapping("/meetings/{id}/signin-dashboard")
    public ApiResponse<Map<String, Object>> signInDashboard(@PathVariable Long id) {
        return ApiResponse.ok(meetingService.dashboard(id));
    }
}
