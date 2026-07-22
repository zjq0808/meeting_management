# 消息发送场景说明

本文档按当前后端代码梳理会议管理系统中所有需要发送消息的业务场景。当前项目的消息发送入口为 `NotificationSender.send()`，实际实现 `LoggingNotificationSender` 只打印日志模拟发送；业务消息都会先写入 `t_meeting_notification`。

## 1. 当前发送机制

相关代码：

- `backend/src/main/java/com/example/meeting/service/NotificationSender.java`
- `backend/src/main/java/com/example/meeting/service/LoggingNotificationSender.java`
- `backend/src/main/java/com/example/meeting/service/MeetingService.java`
- `backend/src/main/resources/mapper/MeetingMapper.xml`

统一发送流程：

1. `MeetingService.notice(...)` 构造消息对象。
2. `meetingMapper.insertNotification(notice)` 写入 `t_meeting_notification`。
3. `notificationSender.send(notice)` 执行发送。

当前发送实现：

```java
log.info("mock notification sent: {}", notification);
```

也就是说，目前没有真正对接企业微信、短信或邮件，只是日志模拟发送。

## 2. 消息字段

消息对象字段和数据库字段对应关系：

| 消息对象字段 | 数据库字段 | 说明 |
| --- | --- | --- |
| meetingId | meeting_id | 会议ID |
| topicId | topic_id | 议题ID，可为空 |
| tousers | tousers | 接收人用户ID，当前通常是单个用户ID |
| title | title | 消息标题 |
| content | msg_body | 消息正文 |
| jobId | job_id | 本地生成的任务ID |
| 自动写入 | send_time | 写库时使用 `now()` |
| 自动写入 | created_at | 写库时使用 `now()` |

## 3. 发送场景清单

### 3.1 发布会议后通知部门秘书遴选参会人员

触发接口：

```http
POST /api/meetings/{meetingId}/publish
```

触发方法：

- `MeetingService.publish(...)`
- `MeetingService.notifySecretary(...)`

触发条件：

- 会议至少有一个议题。
- 会议发布成功，状态改为 `PUBLISHED`。
- 后端遍历每个议题的汇报部门和参会部门。
- 每个部门根据 `t_meeting_authority` 查找 `SECRETARY`。

接收人：

- 议题汇报部门秘书。
- 议题参会部门秘书。
- 同一议题中重复部门只通知一次。

标题：

```text
会议参会遴选通知
```

内容：

```text
请为议题《{议题标题}》遴选参会人员
```

用途：

- 提醒部门秘书进入会议，选择该议题的汇报人或参会人。

### 3.2 发布会议时部门未匹配到秘书，通知管理员

触发接口：

```http
POST /api/meetings/{meetingId}/publish
```

触发方法：

- `MeetingService.publish(...)`
- `MeetingService.notifyAdmins(...)`

触发条件：

- 发布会议时，某个汇报部门或参会部门没有找到部门秘书。

接收人：

- 所有 `ADMIN` 用户。

标题：

```text
联系人缺失提醒
```

内容：

```text
部分部门未匹配到秘书，请手动补充联系人：{部门ID集合}
```

用途：

- 提醒管理员维护部门秘书权限，避免后续无人处理参会人员遴选。

### 3.3 提交议题参会人员后通知提交人

触发接口：

```http
POST /api/topics/{topicId}/attendees
```

触发方法：

- `MeetingService.submitAttendees(...)`
- `MeetingService.notifySubmitSuccess(...)`

触发条件：

- 当前用户成功提交某个议题的汇报人或参会人名单。

接收人：

- 当前提交人。

标题：

```text
参会名单提交成功
```

内容：

```text
议题《{议题标题}》参会名单已同步
```

用途：

- 给操作人一个业务确认消息。

### 3.4 手动通知入场准备

触发接口：

```http
POST /api/topics/{topicId}/notify-preparation
```

请求体示例：

```json
{
  "attendeeUserIds": ["U0004", "U0006"]
}
```

触发方法：

- `MeetingService.notifyTopicPreparation(...)`

触发条件：

- 会议不是 `DRAFT`。
- 当前用户有权限操作该议题。
- 请求体没有传 `shareUserIds`。
- 议题下存在 `confirm_status = 'PENDING'` 的参会记录。

接收人：

- 如果传了 `attendeeUserIds`：只通知指定用户中仍为 `PENDING` 的参会人员。
- 如果没传 `attendeeUserIds`：通知该议题下全部 `PENDING` 参会人员。

标题：

```text
会议入场准备通知
```

内容：

```text
您已被选为议题《{议题标题}》的{汇报人/参会人}，请做好入场准备。
```

用途：

- 在参会人员被选择但尚未确认前，提醒其做入场准备。

### 3.5 分享议题处理权

触发接口：

```http
POST /api/topics/{topicId}/notify-preparation
```

请求体示例：

```json
{
  "shareUserIds": ["U0006"]
}
```

触发方法：

- `MeetingService.notifyTopicPreparation(...)`
- `MeetingService.shareTopic(...)`

触发条件：

- 会议不是 `DRAFT`。
- 当前用户有权限操作该议题。
- 请求体传入 `shareUserIds`。
- 分享对象必须是当前用户同部门人员。

接收人：

- `shareUserIds` 指定的用户。

标题：

```text
会议议题分享通知
```

内容：

```text
您已收到议题《{议题标题}》的分享处理权限，可协助选择汇报人和参会人。
```

用途：

- 给同部门人员授予协助处理议题人员选择的业务权限。
- 该消息不仅用于提醒，也用于后端权限判断。

相关权限逻辑：

- `hasSharedTopicAccess(...)` 会通过 `t_meeting_notification` 中的 `会议议题分享通知` 判断用户是否拥有分享处理权。
- `listSharedTasks(...)` 会通过该通知查询用户可处理的分享议题。
- `listMeetingsForUser(...)` 会让拥有分享通知的用户看到对应会议。

### 3.6 确认参会人员后通知参会人

触发接口：

```http
POST /api/meetings/{meetingId}/attendees/confirm
```

触发方法：

- `MeetingService.confirmAttendees(...)`

触发条件：

- 当前用户角色是 `SECRETARY` 或 `LEADER`。
- 会议不是 `DRAFT`。
- 存在当前用户自己选择过、且仍为 `PENDING` 的参会记录。

接收人：

- 被确认参会的用户。

标题：

```text
会议参会确认通知
```

内容：

```text
您已被确认参加议题《{议题标题}》，请按会议通知准时参会。
```

用途：

- 通知参会人已被正式确认。
- 确认后参会记录变为 `CONFIRMED`，用户才会在个人会议列表看到会议并可签到。

### 3.7 已发布会议调整议题顺序后通知相关人员

触发接口：

```http
PUT /api/meetings/{meetingId}
```

触发方法：

- `MeetingService.updateMeeting(...)`
- `MeetingService.notifyTopicOrderChanged(...)`

触发条件：

- 当前操作人是 `ADMIN`。
- 会议编辑前状态不是 `DRAFT`，即只在已发布、进行中或已结束会议上触发。
- 被编辑的是已有议题。
- 议题原 `sortNo` 和本次保存后的 `sortNo` 不一致。

不会触发的情况：

- 草稿会议调整议题顺序。
- 新增议题。
- 删除议题。
- 只修改标题、部门、纪要、结论等非排序字段。

接收人：

- 该议题汇报部门对应的部门秘书。
- 该议题参会部门对应的部门秘书。
- 该议题下已选择的所有参会记录人员，包括 `PENDING` 和 `CONFIRMED`。

去重规则：

- 同一用户如果既是部门秘书又是参会人员，只发送一条。
- 同一用户如果同时属于多个接收来源，只发送一条。

标题：

```text
议题顺序调整通知
```

内容：

```text
议题《{议题标题}》顺序已调整为第{新排序号}项，请关注会议议程变化。
```

用途：

- 管理员调整已发布会议的议题顺序后，提醒相关部门秘书和该议题已选择的参会人员关注议程变化。

## 4. 当前不发送消息的场景

以下业务目前没有调用 `NotificationSender.send()`：

| 场景 | 当前行为 |
| --- | --- |
| 创建会议 | 只保存会议和议题，不发消息 |
| 编辑会议 | 一般字段编辑不发消息；已发布会议的已有议题排序变化会发送 `议题顺序调整通知` |
| 导入议题Word | 只写入议题和上传记录，不发消息 |
| 生成签到token | 只返回 token 和 URL，不发消息 |
| 签到成功 | 只更新签到状态，不发消息 |
| 开始议题 | 只更新会议/议题状态，不发消息 |
| 结束议题 | 只更新会议/议题状态，不发消息 |
| 更新议题结论或基础信息 | 只更新议题，不发消息 |
| 查询会议列表/详情/看板 | 只查询数据，不发消息 |

## 5. 注意事项

- `t_meeting_notification.tousers` 当前是文本字段，代码中多数场景只保存单个用户ID。
- `listNotifications(userId)` 在 Mapper 中存在，但当前没有 Controller 接口暴露通知列表。
- `会议议题分享通知` 是权限判断依据，不能只当作普通消息删除，否则会影响分享处理权和会议可见性。
- 当前发送失败不会影响业务流程，因为 `LoggingNotificationSender.send()` 固定返回 `true`，且业务代码没有使用返回值做补偿。
