package com.example.meeting.service;

import com.example.meeting.mapper.UserMapper;
import com.example.meeting.util.BusinessException;
import com.example.meeting.util.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final String groupCode;
    private final Map<String, String> tokens = new ConcurrentHashMap<String, String>();

    public AuthService(UserMapper userMapper, @Value("${meeting.group-code:DEFAULT}") String groupCode) {
        this.userMapper = userMapper;
        this.groupCode = groupCode;
    }

    public Map<String, Object> login(String login) {
        if (login == null || login.trim().length() == 0 || "null".equalsIgnoreCase(login.trim())) {
            throw new BusinessException("请提供 userId 或 username");
        }
        Map<String, Object> user = userMapper.findByLogin(login.trim(), groupCode);
        if (user == null) {
            throw new BusinessException("用户不存在或未同步到企业微信用户表");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, Maps.stringValue(user, "id", "userId", "user_id"));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("token", token);
        result.put("user", sanitize(user));
        return result;
    }

    public Map<String, Object> login(String username, String password) {
        return login(username);
    }

    public Map<String, Object> currentUser(String token) {
        String userId = tokens.get(token);
        return userId == null ? null : sanitize(userMapper.findById(userId, groupCode));
    }

    private Map<String, Object> sanitize(Map<String, Object> user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<String, Object>(user);
        copy.put("userId", Maps.stringValue(copy, "userId", "user_id", "id"));
        copy.put("employeeNo", Maps.stringValue(copy, "employeeNo", "employee_no", "user_id", "id"));
        copy.put("realName", Maps.stringValue(copy, "realName", "real_name", "username"));
        copy.put("departmentId", Maps.stringValue(copy, "departmentId", "department_id"));
        copy.put("departmentName", Maps.stringValue(copy, "departmentName", "department_name"));
        copy.remove("passwordHash");
        copy.remove("password_hash");
        return copy;
    }
}
