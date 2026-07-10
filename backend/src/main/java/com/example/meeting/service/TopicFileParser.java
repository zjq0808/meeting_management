package com.example.meeting.service;

import com.example.meeting.util.BusinessException;
import com.example.meeting.util.Maps;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TopicFileParser {
    public List<Map<String, Object>> parse(MultipartFile file, List<Map<String, Object>> departments) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("议题文件为空");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".doc") && !name.endsWith(".docx")) {
            throw new BusinessException("仅支持 Word 格式议题文件");
        }
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new BusinessException("单文件最大支持 50MB");
        }
        String text = extractText(file, name);
        List<Map<String, Object>> topics = parseText(text, departments);
        if (topics.isEmpty()) {
            throw new BusinessException("未识别到议题，可手动新增议题");
        }
        return topics;
    }

    private String extractText(MultipartFile file, String name) {
        try (InputStream in = file.getInputStream()) {
            if (name.endsWith(".docx")) {
                XWPFDocument doc = new XWPFDocument(in);
                StringBuilder builder = new StringBuilder();
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    builder.append(paragraph.getText()).append('\n');
                }
                doc.close();
                return builder.toString();
            }
            HWPFDocument doc = new HWPFDocument(in);
            WordExtractor extractor = new WordExtractor(doc);
            String text = extractor.getText();
            extractor.close();
            doc.close();
            return text;
        } catch (Exception ex) {
            throw new BusinessException("议题文件解析失败：" + ex.getMessage());
        }
    }

    private List<Map<String, Object>> parseText(String text, List<Map<String, Object>> departments) {
        List<Map<String, Object>> topics = new ArrayList<Map<String, Object>>();
        Map<String, Object> current = null;
        String[] lines = text == null ? new String[0] : text.split("\\r?\\n");
        int sort = 1;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.length() == 0) {
                continue;
            }
            if (isTopicStart(line)) {
                if (current != null && current.get("title") != null) {
                    topics.add(current);
                    sort++;
                }
                current = newTopic(sort);
                String title = stripLabel(line);
                if (title.length() > 0) {
                    current.put("title", title);
                }
                continue;
            }
            if (current == null) {
                current = newTopic(sort);
            }
            applyLabeledValue(current, line, departments);
        }
        if (current != null && current.get("title") != null) {
            topics.add(current);
        }
        return topics;
    }

    private Map<String, Object> newTopic(int sort) {
        Map<String, Object> topic = new HashMap<String, Object>();
        topic.put("sortNo", sort);
        topic.put("topicType", "三重一大");
        return topic;
    }

    private boolean isTopicStart(String line) {
        return line.startsWith("议题标题") || line.matches("^(议题\\s*[一二三四五六七八九十0-9]+|[0-9]+[、.．-]).*");
    }

    private String stripLabel(String line) {
        return line.replaceFirst("^(议题标题|议题\\s*[一二三四五六七八九十0-9]+|[0-9]+[、.．-])[:：\\s]*", "").trim();
    }

    private void applyLabeledValue(Map<String, Object> topic, String line, List<Map<String, Object>> departments) {
        String value = line.replaceFirst("^[^:：]+[:：]", "").trim();
        if (line.startsWith("议题类型")) {
            topic.put("topicType", value);
        } else if (line.startsWith("议题标题")) {
            topic.put("title", value);
        } else if (line.startsWith("汇报部门") || line.startsWith("牵头部门")) {
            topic.put("reportDepartmentName", value);
            topic.put("reportDepartmentId", matchDepartmentId(value, departments));
        } else if (line.startsWith("参会部门")) {
            topic.put("participantDepartments", value);
            topic.put("participantDepartmentIds", matchDepartmentIds(value, departments));
        } else if (line.startsWith("议题简述") || line.startsWith("简述")) {
            topic.put("summary", value);
        } else if (topic.get("summary") == null && topic.get("title") != null) {
            topic.put("summary", line);
        }
    }

    private String matchDepartmentId(String text, List<Map<String, Object>> departments) {
        for (Map<String, Object> department : departments) {
            String name = Maps.stringValue(department, "name");
            if (name != null && text.contains(name)) {
                return Maps.stringValue(department, "id", "deptId");
            }
        }
        return null;
    }

    private List<String> matchDepartmentIds(String text, List<Map<String, Object>> departments) {
        List<String> ids = new ArrayList<String>();
        for (Map<String, Object> department : departments) {
            String name = Maps.stringValue(department, "name");
            if (name != null && text.contains(name)) {
                ids.add(Maps.stringValue(department, "id", "deptId"));
            }
        }
        return ids;
    }
}
