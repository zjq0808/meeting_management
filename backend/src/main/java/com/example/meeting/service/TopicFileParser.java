package com.example.meeting.service;

import com.example.meeting.util.BusinessException;
import com.example.meeting.util.Maps;
import com.example.meeting.util.MeetingTopicParserUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class TopicFileParser {
    public List<Map<String, Object>> parse(MultipartFile file, List<Map<String, Object>> departments) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("议题文件为空");
        }
        if (!MeetingTopicParserUtil.isValidDocxFile(file.getOriginalFilename(), file.getSize())) {
            throw new BusinessException("仅支持 docx 格式 Word 议题文件，单文件最大支持 50MB");
        }
        try {
            List<Map<String, Object>> parsedTopics = MeetingTopicParserUtil.parseTopics(file.getInputStream(), file.getOriginalFilename());
            List<Map<String, Object>> topics = normalizeTopics(parsedTopics, departments);
            if (topics.isEmpty()) {
                throw new BusinessException("未识别到议题，可手动新增议题");
            }
            return topics;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("议题文件解析失败：" + ex.getMessage());
        }
    }

    private List<Map<String, Object>> normalizeTopics(List<Map<String, Object>> parsedTopics, List<Map<String, Object>> departments) {
        List<Map<String, Object>> topics = new ArrayList<Map<String, Object>>();
        if (parsedTopics == null) {
            return topics;
        }
        for (Map<String, Object> parsed : parsedTopics) {
            Map<String, Object> topic = new HashMap<String, Object>();
            topic.put("sortNo", Maps.intValue(parsed, "sort_no", "sortNo"));
            topic.put("topicType", Maps.stringValue(parsed, "topic_type", "topicType"));
            topic.put("title", Maps.stringValue(parsed, "title"));
            topic.put("summary", Maps.stringValue(parsed, "summary"));
            topic.put("conclusion", Maps.stringValue(parsed, "conclusion"));
            topic.put("notice", Maps.stringValue(parsed, "notice"));
            topic.put("projectCode", Maps.stringValue(parsed, "project_code", "projectCode"));

            String reportDepartmentText = Maps.stringValue(parsed, "report_dept_name", "reportDepartmentName");
            List<String> reportDepartmentIds = matchDepartmentIds(reportDepartmentText, departments);
            topic.put("reportDepartmentIds", reportDepartmentIds);
            topic.put("reportDepartmentId", join(reportDepartmentIds));
            topic.put("reportDepartmentName", reportDepartmentIds.isEmpty() ? reportDepartmentText : joinNames(reportDepartmentIds, departments));

            String participantDepartmentText = Maps.stringValue(parsed, "participant_dept_name", "participantDepartments");
            List<String> participantDepartmentIds = matchDepartmentIds(participantDepartmentText, departments);
            topic.put("participantDepartmentIds", participantDepartmentIds);
            topic.put("participantDeptId", join(participantDepartmentIds));
            topic.put("participantDepartments", participantDepartmentIds.isEmpty() ? participantDepartmentText : joinNames(participantDepartmentIds, departments));
            topics.add(topic);
        }
        return topics;
    }

    private List<String> matchDepartmentIds(String text, List<Map<String, Object>> departments) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        if (text == null || departments == null) {
            return new ArrayList<String>();
        }
        for (Map<String, Object> department : departments) {
            String name = Maps.stringValue(department, "name");
            if (name != null && text.contains(name)) {
                ids.add(Maps.stringValue(department, "id", "deptId"));
            }
        }
        return new ArrayList<String>(ids);
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null || value.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String joinNames(List<String> ids, List<Map<String, Object>> departments) {
        List<String> names = new ArrayList<String>();
        for (String id : ids) {
            for (Map<String, Object> department : departments) {
                if (id.equals(Maps.stringValue(department, "id", "deptId"))) {
                    String name = Maps.stringValue(department, "name");
                    if (name != null) {
                        names.add(name);
                    }
                    break;
                }
            }
        }
        return join(names);
    }
}
