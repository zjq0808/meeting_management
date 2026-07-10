package com.example.meeting.controller;

import com.example.meeting.dto.ApiResponse;
import com.example.meeting.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DirectoryController {
    private final UserMapper userMapper;
    private final String groupCode;

    public DirectoryController(UserMapper userMapper, @Value("${meeting.group-code:DEFAULT}") String groupCode) {
        this.userMapper = userMapper;
        this.groupCode = groupCode;
    }

    @GetMapping("/departments")
    public ApiResponse<List<Map<String, Object>>> departments() {
        return ApiResponse.ok(userMapper.listDepartments());
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(@RequestParam(required = false) String departmentId,
                                                        @RequestParam(required = false) String role,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userMapper.listUsers(departmentId, roleAuthority(role), participantOnly(role), keyword, groupCode));
    }

    private Integer roleAuthority(String role) {
        if ("ADMIN".equals(role)) {
            return 1;
        }
        if ("LEADER".equals(role)) {
            return 2;
        }
        if ("SECRETARY".equals(role)) {
            return 3;
        }
        return null;
    }

    private Boolean participantOnly(String role) {
        return "PARTICIPANT".equals(role) ? Boolean.TRUE : null;
    }
}
