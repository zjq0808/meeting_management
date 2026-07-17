# 三重一大会议管理系统接口文档（Postman 本地调试）

本文档基于当前后端代码整理，适用于本地 Postman 调通接口。

*通讯录相关接口使用现有的通用接口，支持获取组织架构、获取角色、根据工号获取人员列表等
*移动端、PC端使用接口一致
*查询列表或详情，根据用户权限自动筛选列表，详情页根据权限显示按钮，并高亮显示本人相关议题
*议题导入功能单独开发，解析内容后返回前端json，前端直接展示，确认无误后提交保存

## 1. 会议管理接口

### 1.1 查询会议列表

用途：查询会议首页列表。

```http
GET {{baseUrl}}/meetings
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

权限逻辑：

- `ADMIN`：查看全部会议。
- 非 `ADMIN`：查看本人创建、本人参会、本人收到通知、本人部门涉及的会议。

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "meetingDate": "2026-07-08",
      "meetingTime": "09:00",
      "periodNo": "2026年第1期",
      "location": "第一会议室",
      "leaders": "局领导",
      "content": "三重一大事项审议",
      "status": "DRAFT",
      "createdBy": 1,
      "createdAt": "2026-07-07T10:00:00",
      "updatedAt": "2026-07-07T10:00:00"
    }
  ]
}

```

### 1.2 创建会议

用途：管理员创建会议，可同时传入议题。

```http
POST {{baseUrl}}/meetings
```

请求头：

```text
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

权限：必须是 `role = ADMIN`。

请求体：

```json
{
  "meetingDate": "2026-07-08",
  "meetingTime": "09:00",
  "periodNo": "2026年第1期",
  "location": "第一会议室",
  "leaders": "局领导",
  "content": "三重一大事项审议",
  "topics": [
    {
      "topicType": "重大项目",
      "title": "技术平台升级事项",
      "reportDepartmentId": 2,
      "participantDepartments": "技术规划处、数据中心",
      "participantDepartmentIds": [2, 3],
      "summary": "审议平台升级计划",
      "sortNo": 1
    }
  ]
}
```

关键字段说明：

| 字段 | 说明 |
|---|---|
| `meetingDate` | 会议日期 |
| `meetingTime` | 会议时间 |
| `periodNo` | 会议期数 |
| `location` | 会议地点 |
| `leaders` | 参会领导 |
| `content` | 会议内容 |
| `topics` | 议题数组，可以为空；如果为空，后续可通过导入议题或编辑会议补充 |
| `topicType` | 议题类型 |
| `title` | 议题标题 |
| `reportDepartmentId` | 汇报部门 ID |
| `participantDepartments` | 参会部门名称文本 |
| `participantDepartmentIds` | 参会部门 ID 数组 |
| `summary` | 会议纪要 |
| `sortNo` | 议题排序 |

响应会返回完整会议详情，包括 `topics`。Postman Tests 可保存 ID：

```javascript
const body = pm.response.json();
pm.environment.set("meetingId", body.data.id);
pm.environment.set("topicId", body.data.topics[0].id);
```

### 1.3 修改会议

用途：管理员修改会议基础信息和议题。

```http
PUT {{baseUrl}}/meetings/{{meetingId}}
```

请求头：

```text
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

权限：必须是 `role = ADMIN`。

请求体：和创建会议一致。已有议题需要带 `id`，新议题不带 `id`。

```json
{
  "meetingDate": "2026-07-08",
  "meetingTime": "09:30",
  "periodNo": "2026年第1期",
  "location": "第一会议室",
  "leaders": "局领导",
  "content": "三重一大事项审议",
  "topics": [
    {
      "id": 1,
      "topicType": "重大项目",
      "title": "技术平台升级事项",
      "reportDepartmentId": 2,
      "participantDepartments": "技术规划处、数据中心",
      "participantDepartmentIds": [2, 3],
      "summary": "审议平台升级计划",
      "sortNo": 1
    }
  ]
}
```

### 1.4 查询会议详情

用途：查询会议基础信息、议题、参会人员、签到记录、签到状态。

```http
GET {{baseUrl}}/meetings/{{meetingId}}
```

等价接口：

```http
GET {{baseUrl}}/meetings/{{meetingId}}/detail
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

响应结构重点：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "meetingDate": "2026-07-08",
    "meetingTime": "09:00",
    "periodNo": "2026年第1期",
    "location": "第一会议室",
    "status": "DRAFT",
    "topics": [
      {
        "id": 1,
        "meetingId": 1,
        "topicType": "重大项目",
        "title": "技术平台升级事项",
        "reportDepartmentId": 2,
        "reportDepartmentName": "技术规划处",
        "participantDepartmentIds": [2, 3],
        "summary": "审议平台升级计划",
        "sortNo": 1,
        "status": "WAITING",
        "attendees": []
      }
    ],
    "attendees": [],
    "signIns": [],
    "topicSignStats": [
      {
        "id": 1,
        "sortNo": 1,
        "title": "技术平台升级事项",
        "reportDepartmentName": "技术规划处",
        "signed": false
      }
    ]
  }
}
```

### 1.5 发布会议

用途：管理员发布会议，系统生成部门秘书通知。

```http
POST {{baseUrl}}/meetings/{{meetingId}}/publish
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

业务规则：

- 会议至少需要一个议题。
- 发布后会议状态更新为 `PUBLISHED`。
- 系统按议题汇报部门和参会部门查找部门秘书，写入通知表。
- 如果某些部门未匹配到秘书，会给管理员生成提醒通知。

响应：返回会议详情。

常见失败：

```json
{
  "success": false,
  "message": "会议至少需要一个议题才能发布",
  "data": null
}
```

### 1.6 导入议题 Word 文件

用途：上传 `.doc` 或 `.docx`，系统解析议题。

```http
POST {{baseUrl}}/meetings/{{meetingId}}/topics/import
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

Postman Body：

- 选择 `form-data`
- key：`file`
- 类型：`File`
- value：选择本地 `.doc` 或 `.docx`

限制：

- 仅支持 `.doc`、`.docx`
- 单文件最大 50MB
- 解析字段包括：议题类型、议题标题、汇报部门、参会部门、会议纪要

常见失败：

```json
{
  "success": false,
  "message": "仅支持 Word 格式议题文件",
  "data": null
}
```

```json
{
  "success": false,
  "message": "未识别到议题，可手动新增议题",
  "data": null
}
```

## 2. 参会遴选接口

### 2.1 查询遴选任务

用途：秘书/科组长查看自己需要处理的议题。

```http
GET {{baseUrl}}/meetings/{{meetingId}}/selection-tasks
```

请求头，秘书：

```text
Authorization: Bearer {{secretaryToken}}
```

业务逻辑：

- `SECRETARY`：返回本部门是汇报部门或参会部门的议题。
- `ADMIN`：返回会议全部议题。
- 其他角色：当前返回会议全部议题。

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "meetingId": 1,
      "title": "技术平台升级事项",
      "reportDepartmentId": 2,
      "reportDepartmentName": "技术规划处",
      "sortNo": 1,
      "status": "WAITING"
    }
  ]
}
```

### 2.2 提交参会名单

用途：秘书提交科组长名单，科组长提交普通参会人员名单。

```http
POST {{baseUrl}}/topics/{{topicId}}/attendees
```

请求头，秘书提交科组长：

```text
Authorization: Bearer {{secretaryToken}}
Content-Type: application/json
```

请求体，管理员或秘书选择汇报人：

```json
{
  "userIds": [4],
  "attendeeType": "REPORT"
}
```

请求头，管理员或秘书选择参会人员：

```text
Authorization: Bearer {{leaderToken}}
Content-Type: application/json
```

请求体，选择参会人员：

```json
{
  "userIds": [6],
  "attendeeType": "PARTAKE"
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `userIds` | 被选择的用户 ID 数组，不能为空 |
| `attendeeType` | `REPORT` 或 `PARTAKE`。不传时，系统按当前用户角色推断 |

业务规则：

- `userIds` 不能为空。
- 同一议题最大参会人数由配置 `meeting.max-attendees-per-topic` 控制，默认 60。
- 同一提交人再次提交同类型名单时，会先删除该提交人的旧名单再插入新名单。

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "topic": {
      "id": 1,
      "title": "技术平台升级事项"
    },
    "attendees": [
      {
        "id": 1,
        "topicId": 1,
        "userId": 4,
        "attendeeType": "REPORT",
        "selectedBy": 2,
        "username": "规划科组长",
        "realName": "规划科组长",
        "employeeNo": "U0004",
        "departmentName": "技术规划处"
      }
    ]
  }
}
```

常见失败：

```json
{
  "success": false,
  "message": "请至少勾选一名参会人员",
  "data": null
}
```

## 3. 签到接口

### 3.1 扫码签到

用途：参会人员扫码后签到。Postman 中直接传 `signToken` 模拟扫码。

```http
POST {{baseUrl}}/sign-in
```

请求头，参会人员：

```text
Authorization: Bearer {{participantToken}}
Content-Type: application/json
```

请求体：

```json
{
  "token": "{{signToken}}"
}
```

业务规则：

- token 必须存在且未过期。
- 当前用户必须在该会议至少一个议题的 `meeting_topic_attendee` 参会名单中。
- 同一用户同一议题不能重复签到。
- 如果签到用户所属部门是议题汇报部门，会更新该议题 `report_department_signed = true`。

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "message": "签到成功",
    "topics": [
      {
        "id": 1,
        "title": "技术平台升级事项"
      }
    ]
  }
}
```

常见失败：

```json
{
  "success": false,
  "message": "二维码无效",
  "data": null
}
```

```json
{
  "success": false,
  "message": "身份不在本次会议参会名单中或已完成签到",
  "data": null
}
```

## 4. 会议控制与结论接口

### 4.1 开始议题

用途：管理员开始某个议题。

```http
POST {{baseUrl}}/topics/{{topicId}}/start
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

业务规则：

- 同一会议只能有一个 `RUNNING` 议题。
- 开始后会议状态变为 `IN_PROGRESS`。
- 议题状态变为 `RUNNING`，写入 `start_time`。

响应：返回会议 dashboard 数据。

常见失败：

```json
{
  "success": false,
  "message": "请先结束当前议题",
  "data": null
}
```

### 4.2 结束议题

用途：管理员结束正在讨论的议题。

```http
POST {{baseUrl}}/topics/{{topicId}}/end
```

请求头：

```text
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

请求体，不手动指定时长：

```json
{}
```

请求体，手动指定实际时长：

```json
{
  "actualMinutes": 12
}
```

业务规则：

- 议题必须已开始且状态为 `RUNNING`。
- 结束后议题状态变为 `FINISHED`，写入 `end_time` 和 `actual_minutes`。
- 如果全部议题都已结束，会议状态变为 `FINISHED`。

响应：返回会议 dashboard 数据。

常见失败：

```json
{
  "success": false,
  "message": "议题尚未开始",
  "data": null
}
```

### 4.3 更新议题信息

用途：管理员直接维护议题名称、议题类型、会议纪要和会议结论。

```http
PUT {{baseUrl}}/topics/{{topicId}}
```

请求头：

```text
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

请求体：

```json
{
  "title": "技术平台升级事项",
  "topicType": "重大决策",
  "summary": "同意按程序推进",
  "conclusion": "同意按程序推进"
}
```

字段说明：

| 字段 | 是否必填 | 说明 |
|---|---|---|
| `title` | 是 | 议题名称 |
| `topicType` | 是 | 议题类型 |
| `summary` | 否 | 会议纪要 |
| `conclusion` | 否 | 会议结论文本 |

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "技术平台升级事项",
    "topicType": "重大决策",
    "summary": "同意按程序推进",
    "conclusion": "同意按程序推进"
  }
}
```

## 5. 看板与进程接口

### 5.1 会议控制台 dashboard

用途：管理员查看当前议题、签到人数、参会人数、议题签到状态等。

```http
GET {{baseUrl}}/meetings/{{meetingId}}/dashboard
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

响应重点字段：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "status": "IN_PROGRESS",
    "currentTopic": {
      "id": 1,
      "sortNo": 1,
      "status": "RUNNING"
    },
    "signedCount": 1,
    "attendeeCount": 2,
    "topicCount": 1,
    "signedTopicCount": 1,
    "topicSignStats": [
      {
        "id": 1,
        "sortNo": 1,
        "title": "技术平台升级事项",
        "reportDepartmentName": "技术规划处",
        "signed": true
      }
    ],
    "topics": []
  }
}
```

### 5.2 签到看板

用途：签到页面查看总议题数、已签到议题数、各议题汇报部门签到状态。当前实现复用 dashboard 数据。

```http
GET {{baseUrl}}/meetings/{{meetingId}}/signin-dashboard
```

请求头：

```text
Authorization: Bearer {{adminToken}}
```

响应字段同 dashboard。
