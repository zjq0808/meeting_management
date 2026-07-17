package com.example.meeting.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 三重一大会议议题Word解析工具类
 * <p>
 * 功能说明：
 * 1. 解析 docx 格式的 Word 文档，提取会议议题信息
 * 2. 支持两种议题格式：普通议题 和 联合采购项目
 * 3. 自动识别议题标题、汇报部门、参会部门、会议内容、会议结论
 * <p>
 * 使用示例：
 * <pre>
 * File file = new File("议题文档.docx");
 * List&lt;Map&lt;String, Object&gt;&gt; topics = MeetingTopicParserUtil.parseTopics(file);
 * String json = MeetingTopicParserUtil.toJsonString(topics);
 * </pre>
 *
 * @author demo
 * @version 1.0
 */
public class MeetingTopicParserUtil {

    // ==================== 正则表达式常量 ====================

    /**
     * 普通议题正则：类型：标题（A，B，C，汇报部门，参会部门）
     * <p>
     * 示例：现场技术支持项目：邮件系统升级（P001，100万元，公开招标，技术部汇报，财务部参会）
     * <p>
     * 捕获组：
     * group(1) = 类型（如：现场技术支持项目）
     * group(2) = 标题（如：邮件系统升级）
     * group(3) = 项目编号
     * group(4) = 金额
     * group(5) = 采购方式
     * group(6) = 汇报部门
     * group(7) = 参会部门
     */
    private static final Pattern TOPIC_NORMAL =
            Pattern.compile("^([^：:]+)[：:](.+?)（([^，]+)，([^，]+)，([^，]+)，([^，]+)汇报，([^，]+)参会）");

    /**
     * 联合采购项目正则：联合采购项目（N项）：标题（合计X，采购方式，汇报部门，参会部门）
     * <p>
     * 示例：联合采购项目（2项）：软件采购（合计50万元，公开招标，技术部汇报，财务部参会）
     * <p>
     * 捕获组：
     * group(1) = 类型（如：联合采购项目（2项））
     * group(2) = 标题
     * group(3) = 合计金额
     * group(4) = 采购方式
     * group(5) = 汇报部门
     * group(6) = 参会部门
     */
    private static final Pattern TOPIC_JOINT =
            Pattern.compile("^(联合采购项目（\\d+项）)[：:](.+?)（([^，]+)，([^，]+)，([^，]+)汇报，([^，]+)参会）");

    /**
     * 拟结论标记正则
     */
    private static final Pattern CONCLUSION_PATTERN =
            Pattern.compile("拟结论[：:](.*)");

    /**
     * 项目编号提取正则
     */
    private static final Pattern PROJECT_CODE_PATTERN =
            Pattern.compile("[（(](\\w+)[）)]");

    // ==================== 常量配置 ====================

    /** 支持的文件扩展名 */
    private static final String SUPPORTED_EXTENSION = "docx";

    /** 最大文件大小：50MB */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // ==================== 私有构造方法 ====================

    private MeetingTopicParserUtil() {
        // 工具类私有构造，防止实例化
    }

    // ==================== 公开 API ====================

    /**
     * 解析议题Word文件（通过File对象）
     *
     * @param file Word文件
     * @return 解析后的议题列表，每个议题为 Map 结构
     * @throws IllegalArgumentException 文件为空、格式不支持或解析失败时抛出
     */
    public static List<Map<String, Object>> parseTopics(File file) {
        // 参数校验
        validateFile(file);

        try (FileInputStream inputStream = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return parseDocument(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Word文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析议题Word文件（通过InputStream）
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名（用于验证格式）
     * @return 解析后的议题列表，每个议题为 Map 结构
     * @throws IllegalArgumentException 文件输入流为空、格式不支持或解析失败时抛出
     */
    public static List<Map<String, Object>> parseTopics(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            throw new IllegalArgumentException("文件输入流不能为空");
        }
        validateFileExtension(fileName);

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return parseDocument(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Word文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否为有效的 docx 格式
     *
     * @param file File对象
     * @return true表示有效，false表示无效
     */
    public static boolean isValidDocxFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String filename = file.getName();
        if (filename == null) {
            return false;
        }
        if (!filename.toLowerCase().endsWith("." + SUPPORTED_EXTENSION)) {
            return false;
        }
        return file.length() > 0 && file.length() <= MAX_FILE_SIZE;
    }

    /**
     * 检查文件是否为有效的 docx 格式（通过文件名和文件大小）
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return true表示有效，false表示无效
     */
    public static boolean isValidDocxFile(String fileName, long fileSize) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        if (!fileName.toLowerCase().endsWith("." + SUPPORTED_EXTENSION)) {
            return false;
        }
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE;
    }

    /**
     * 检查解析结果是否包含有效的议题数据
     *
     * @param topics 解析结果列表
     * @return true表示包含有效数据
     */
    public static boolean hasValidTopics(List<Map<String, Object>> topics) {
        return topics != null && !topics.isEmpty();
    }

    /**
     * 将解析结果转换为 JSON 字符串
     *
     * @param topics 解析结果列表
     * @return JSON 字符串
     */
    public static String toJsonString(List<Map<String, Object>> topics) {
        if (topics == null || topics.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < topics.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(mapToJson(topics.get(i)));
        }
        json.append("]");
        return json.toString();
    }

    // ==================== 私有方法 ====================

    /**
     * 验证文件参数
     */
    private static void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + file.getAbsolutePath());
        }
        validateFileExtension(file.getName());
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过50MB限制");
        }
    }

    /**
     * 验证文件扩展名
     */
    private static void validateFileExtension(String fileName) {
        if (fileName == null || !fileName.toLowerCase().endsWith("." + SUPPORTED_EXTENSION)) {
            throw new IllegalArgumentException("仅支持 " + SUPPORTED_EXTENSION + " 格式的Word文件");
        }
    }

    /**
     * 解析 Word 文档
     */
    private static List<Map<String, Object>> parseDocument(XWPFDocument document) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        int sortNo = 0;
        Map<String, Object> currentTopic = null;
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder conclusionBuilder = new StringBuilder();
        boolean inConclusion = false;
        boolean foundAny = false;

        for (XWPFParagraph para : paragraphs) {
            String text = para.getText().trim();
            if (text.isEmpty()) {
                continue;
            }

            // 1. 判断是否为议题标题
            TopicHeaderInfo headerInfo = parseTopicHeader(text);
            if (headerInfo != null && headerInfo.isValid) {
                foundAny = true;

                // 保存上一个议题
                if (currentTopic != null) {
                    finishAndAddTopic(currentTopic, contentBuilder.toString(), conclusionBuilder.toString(), result);
                }

                // 创建新议题
                sortNo++;
                currentTopic = buildTopicMap(sortNo, headerInfo, text);

                // 重置缓存
                contentBuilder = new StringBuilder();
                conclusionBuilder = new StringBuilder();
                inConclusion = false;
                continue;
            }

            // 2. 检测结论标记
            Matcher conclusionMatcher = CONCLUSION_PATTERN.matcher(text);
            if (conclusionMatcher.find() && currentTopic != null) {
                inConclusion = true;
                String conclusionText = conclusionMatcher.group(1).trim();
                if (!conclusionText.isEmpty()) {
                    appendWithSpace(conclusionBuilder, conclusionText);
                }
                continue;
            }

            // 3. 收集内容
            if (currentTopic != null) {
                if (inConclusion) {
                    // 继续收集结论内容（排除已识别的标题行）
                    if (!text.startsWith("拟结论") && !isTopicHeader(text)) {
                        appendWithSpace(conclusionBuilder, text);
                    }
                } else {
                    // 收集正文内容（排除已识别的标题行）
                    if (!isTopicHeader(text)) {
                        if (contentBuilder.length() > 0) {
                            contentBuilder.append("\n");
                        }
                        contentBuilder.append(text);
                    }
                }
            }
        }

        // 保存最后一个议题
        if (currentTopic != null) {
            finishAndAddTopic(currentTopic, contentBuilder.toString(), conclusionBuilder.toString(), result);
        }

        // 输出解析结果统计
        if (!foundAny) {
            System.out.println("⚠️ 警告：未匹配到任何议题标题");
        } else {
            System.out.println("✅ 成功解析 " + result.size() + " 个议题");
        }

        return result;
    }

    /**
     * 解析议题标题，返回标题信息
     *
     * @param text 段落文本
     * @return 标题信息对象，解析失败返回 null
     */
    private static TopicHeaderInfo parseTopicHeader(String text) {
        // 1. 先尝试匹配联合采购项目
        Matcher jointMatcher = TOPIC_JOINT.matcher(text);
        if (jointMatcher.find() && text.contains("汇报") && text.contains("参会")) {
            TopicHeaderInfo info = new TopicHeaderInfo();
            info.isValid = true;
            // 类型中去掉括号内容，如（2项）、（3项）
            String type = jointMatcher.group(1).trim();
            type = type.replaceAll("（\\d+项）", "").trim();
            info.type = type;
            info.title = jointMatcher.group(2).trim();
            info.reportDept = jointMatcher.group(5).trim();
            info.participantDept = jointMatcher.group(6).trim();
            info.summary = jointMatcher.group(3).trim() + "，" + jointMatcher.group(4).trim();
            info.projectCode = extractProjectCode(text);
            return info;
        }

        // 2. 再尝试匹配普通议题
        Matcher normalMatcher = TOPIC_NORMAL.matcher(text);
        if (normalMatcher.find() && text.contains("汇报") && text.contains("参会")) {
            TopicHeaderInfo info = new TopicHeaderInfo();
            info.isValid = true;
            info.type = normalMatcher.group(1).trim();
            info.title = normalMatcher.group(2).trim();
            info.reportDept = normalMatcher.group(6).trim();
            info.participantDept = normalMatcher.group(7).trim();
            info.summary = normalMatcher.group(3).trim() + "，" +
                    normalMatcher.group(4).trim() + "，" +
                    normalMatcher.group(5).trim();
            info.projectCode = normalMatcher.group(3).trim();
            return info;
        }

        return null;
    }

    /**
     * 判断文本是否为议题标题
     *
     * @param text 段落文本
     * @return true 表示是议题标题
     */
    private static boolean isTopicHeader(String text) {
        return (TOPIC_NORMAL.matcher(text).find() && text.contains("汇报") && text.contains("参会")) ||
                (TOPIC_JOINT.matcher(text).find() && text.contains("汇报") && text.contains("参会"));
    }

    /**
     * 提取项目编号（括号内的字母数字组合）
     *
     * @param text 文本
     * @return 项目编号，未找到返回 null
     */
    private static String extractProjectCode(String text) {
        Matcher m = PROJECT_CODE_PATTERN.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 构建议题 Map 对象
     */
    private static Map<String, Object> buildTopicMap(int sortNo, TopicHeaderInfo headerInfo, String notice) {
        Map<String, Object> topic = new LinkedHashMap<>();
        topic.put("sort_no", sortNo);//序号
        topic.put("topic_type", headerInfo.type);//类型
        topic.put("title", headerInfo.title);//标题
        topic.put("report_dept_name", headerInfo.reportDept);//汇报部门
        topic.put("participant_dept_name", headerInfo.participantDept);//参会部门
        topic.put("summary", headerInfo.summary);//摘要
        topic.put("notice", notice);//通知标题
        if (headerInfo.projectCode != null) {
            topic.put("project_code", headerInfo.projectCode);
        }
        return topic;
    }

    /**
     * 带空格的字符串追加（辅助方法）
     */
    private static void appendWithSpace(StringBuilder sb, String text) {
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(text);
    }

    /**
     * 完成议题组装并添加到结果列表
     *
     * @param topic      议题 Map
     * @param content    会议内容
     * @param conclusion 会议结论
     * @param result     结果列表
     */
    private static void finishAndAddTopic(Map<String, Object> topic,
                                          String content,
                                          String conclusion,
                                          List<Map<String, Object>> result) {
        String cleanContent = content.replaceAll("\\s+", " ").trim();
        String cleanConclusion = conclusion.replaceAll("\\s+", " ").trim();

        // 如果结论为空，尝试从内容中提取（兜底方案）
        if (cleanConclusion.isEmpty() && cleanContent.contains("拟结论")) {
            int idx = cleanContent.indexOf("拟结论");
            if (idx > 0) {
                String after = cleanContent.substring(idx);
                int endIdx = after.indexOf("。");
                if (endIdx > 0) {
                    cleanConclusion = after.substring(0, endIdx + 1)
                            .replace("拟结论：", "")
                            .replace("拟结论:", "")
                            .trim();
                    cleanContent = cleanContent.substring(0, idx).trim();
                }
            }
        }

        topic.put("summary", cleanContent);
        topic.put("conclusion", cleanConclusion);
        result.add(topic);
    }

    /**
     * 将 Map 转换为 JSON 字符串（辅助方法）
     */
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        int fieldCount = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (fieldCount > 0) {
                json.append(",");
            }
            String key = entry.getKey();
            Object value = entry.getValue();
            json.append("\"").append(key).append("\":");
            json.append(escapeJsonValue(value));
            fieldCount++;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 转义 JSON 值（辅助方法）
     */
    private static String escapeJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        String str = value.toString();
        return "\"" + str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    // ==================== 内部类 ====================

    /**
     * 议题标题信息内部类
     */
    private static class TopicHeaderInfo {
        boolean isValid = false;          // 是否有效
        String type;                      // 议题类型
        String title;                     // 议题标题
        String reportDept;                // 汇报部门
        String participantDept;           // 参会部门
        String summary;                   // 摘要（项目编号 + 金额 + 采购方式）
        String projectCode;               // 项目编号
    }
}
