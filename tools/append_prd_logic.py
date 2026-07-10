import copy
import html
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
ET.register_namespace("w", W)


def qn(tag):
    return "{%s}%s" % (W, tag)


def el(tag, attrs=None, text=None):
    node = ET.Element(qn(tag), attrs or {})
    if text is not None:
        node.text = text
    return node


def paragraph(text="", style=None, bold=False, size=None, color=None, page_break=False):
    p = el("p")
    ppr = el("pPr")
    if style:
        ppr.append(el("pStyle", {qn("val"): style}))
    spacing = el("spacing", {qn("after"): "120", qn("line"): "276", qn("lineRule"): "auto"})
    ppr.append(spacing)
    p.append(ppr)
    if page_break:
        r = el("r")
        r.append(el("br", {qn("type"): "page"}))
        p.append(r)
    if text:
        r = el("r")
        rpr = el("rPr")
        if bold:
            rpr.append(el("b"))
        if size:
            rpr.append(el("sz", {qn("val"): str(size * 2)}))
            rpr.append(el("szCs", {qn("val"): str(size * 2)}))
        if color:
            rpr.append(el("color", {qn("val"): color}))
        if list(rpr):
            r.append(rpr)
        t = el("t")
        if text.startswith(" ") or text.endswith(" "):
            t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        t.text = text
        r.append(t)
        p.append(r)
    return p


def bullet(text):
    p = paragraph()
    ppr = p.find(qn("pPr"))
    ppr.append(el("ind", {qn("left"): "420", qn("hanging"): "210"}))
    r1 = el("r")
    t1 = el("t")
    t1.text = "• "
    r1.append(t1)
    p.append(r1)
    r2 = el("r")
    t2 = el("t")
    t2.text = text
    r2.append(t2)
    p.append(r2)
    return p


def cell(text, width, bold=False, fill=None):
    tc = el("tc")
    tcpr = el("tcPr")
    tcpr.append(el("tcW", {qn("w"): str(width), qn("type"): "dxa"}))
    tcpr.append(el("tcMar"))
    if fill:
        tcpr.append(el("shd", {qn("fill"): fill}))
    tc.append(tcpr)
    for line in str(text).split("\n"):
        tc.append(paragraph(line, bold=bold))
    return tc


def table(headers, rows, widths):
    tbl = el("tbl")
    tblpr = el("tblPr")
    tblpr.append(el("tblW", {qn("w"): str(sum(widths)), qn("type"): "dxa"}))
    tblpr.append(el("tblInd", {qn("w"): "120", qn("type"): "dxa"}))
    borders = el("tblBorders")
    for side in ["top", "left", "bottom", "right", "insideH", "insideV"]:
        borders.append(el(side, {qn("val"): "single", qn("sz"): "4", qn("space"): "0", qn("color"): "D0D7DE"}))
    tblpr.append(borders)
    tbl.append(tblpr)
    grid = el("tblGrid")
    for width in widths:
        grid.append(el("gridCol", {qn("w"): str(width)}))
    tbl.append(grid)
    tr = el("tr")
    for i, h in enumerate(headers):
        tr.append(cell(h, widths[i], bold=True, fill="E8EEF5"))
    tbl.append(tr)
    for row in rows:
        tr = el("tr")
        for i, value in enumerate(row):
            tr.append(cell(value, widths[i]))
        tbl.append(tr)
    return tbl


def add_many(body, items):
    for item in items:
        body.append(item)


def build_appendix():
    nodes = []
    nodes.append(paragraph("", page_break=True))
    nodes.append(paragraph("四、现有系统逻辑补充与风险校验", style="Heading1", bold=True, size=16, color="2E74B5"))
    nodes.append(paragraph("本章节依据当前已实现的 Spring Boot + MyBatis + MySQL 后端、Vue 3 前端和初始化 SQL 编写，用于把原 PRD 中的业务需求、数据库表、接口调用和角色操作路径串联起来。更新时间：2026-07-06。"))

    nodes.append(paragraph("4.1 当前实现边界", style="Heading2", bold=True, size=13, color="2E74B5"))
    for text in [
        "系统采用本地账号登录与 Bearer token 鉴权，企业微信 OAuth/SSO 暂未真实接入，wecom_user_id 仅作为预留字段。",
        "企业微信和短信通知暂由 NotificationSender 适配器预留，默认实现只落库并记录发送日志，不真实外发。",
        "本地数据库默认使用 MySQL，Kingbase/PostgreSQL 兼容脚本保留，测试环境使用 H2。",
        "会议进程采用前端轮询读取，参会端只展示当前议题序号和已结束议题时长，不展示议题内容。",
        "移动端会议详情分享仍要求登录，不提供免登录公开链接。"
    ]:
        nodes.append(bullet(text))

    nodes.append(paragraph("4.2 角色定义与权限口径", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["角色", "系统角色值", "核心职责", "当前可见范围", "关键限制"],
        [
            ["会议管理员", "ADMIN", "创建会议、导入/编辑议题、发布通知、生成签到二维码、控制议题开始结束、保存结论、查看统计。", "可查看全部会议。", "创建和编辑已做管理员校验；发布、导入、排序、签到二维码、控制台类接口建议继续补充管理员校验。"],
            ["邀请参会人员", "SECRETARY / LEADER", "收到通知后遴选人员。部门秘书选择科组长，科组长选择参会人员。", "当前按本人通知、本人参会、本人创建、所属部门涉及议题过滤会议列表。", "直接访问详情和提交参会名单时，需要增加后端关系校验，避免知道 ID 后越权操作。"],
            ["参会人员", "PARTICIPANT", "查看本人相关会议、扫码签到、查看会议进程、查看会议结论。", "列表仅显示本人被选为参会人或所属部门相关会议。", "移动进程不显示议题内容；扫码签到必须已在 meeting_topic_attendee 名单中。"]
        ],
        [1600, 1450, 2850, 2050, 2600]
    ))

    nodes.append(paragraph("4.3 状态模型", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["对象", "状态", "触发动作", "影响"],
        [
            ["会议 meeting", "DRAFT", "管理员创建或保存会议。", "可编辑；未发布时不会生成通知。"],
            ["会议 meeting", "PUBLISHED", "调用 POST /api/meetings/{id}/publish。", "生成站内通知，秘书/科组长可进入遴选。"],
            ["会议 meeting", "IN_PROGRESS", "管理员开始任一议题。", "移动端进程显示会议进行中。"],
            ["会议 meeting", "FINISHED", "所有议题结束后自动置为结束。", "可查看统计与结论。"],
            ["议题 meeting_topic", "WAITING / RUNNING / FINISHED", "开始议题、结束议题。", "同一会议只允许一个 RUNNING 议题。"],
            ["通知 meeting_notification", "PENDING / SENT / FAILED / READ", "通知创建、发送、读取。", "当前默认写入 SENT，READ 尚未形成完整操作入口。"],
            ["签到 meeting_sign_in", "SIGNED", "扫码且身份匹配。", "唯一约束避免同一用户同一议题重复签到。"]
        ],
        [1700, 2400, 3000, 3200]
    ))

    nodes.append(paragraph("4.4 数据库表与逻辑关系", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["表名", "业务含义", "关键字段", "关系说明"],
        [
            ["sys_department", "组织部门字典。", "id, name, sort_no", "被 sys_user.department_id、meeting_topic.report_department_id、meeting_topic_department.department_id、meeting_notification.receiver_department_id 引用。"],
            ["sys_user", "系统用户、角色、工号和企业微信预留身份。", "id, username, employee_no, role, department_id, wecom_user_id", "作为会议创建人、通知接收人、参会名单人员、签到人员、名单选择提交人。"],
            ["meeting", "会议主表。", "id, meeting_date, meeting_time, period_no, status, sign_token, created_by", "一场会议包含多个议题；存储签到二维码 token 和会议状态。"],
            ["meeting_topic", "会议议题。", "meeting_id, title, report_department_id, sort_no, status, start_time, end_time, actual_minutes, conclusion", "一条议题归属一场会议；汇报部门来自 sys_department；议题状态驱动会议进程。"],
            ["meeting_topic_department", "议题涉及的参会部门。", "topic_id, department_id", "多对多关系：一个议题可涉及多个部门，一个部门可参与多个议题。用于通知和列表过滤。"],
            ["meeting_topic_attendee", "议题参会名单。", "topic_id, user_id, attendee_type, selected_by", "记录秘书/科组长提交的人员；签到时以此表判断用户是否有资格签到。"],
            ["meeting_sign_in", "签到记录。", "meeting_id, topic_id, user_id, sign_status, signed_at, identity_source", "同一 meeting + topic + user 唯一；签到看板统计来源。"],
            ["meeting_notification", "站内通知与外部通知发送记录。", "meeting_id, topic_id, receiver_user_id, receiver_department_id, channel, status", "发布会议和提交名单时写入；也用于非管理员会议列表的本人相关过滤。"],
            ["meeting_uploaded_file", "议题文件上传与解析记录。", "meeting_id, file_name, file_size, parser_status, parser_message", "记录 Word 导入是否成功，失败时保留错误信息供追踪。"]
        ],
        [1700, 2200, 2600, 3600]
    ))
    nodes.append(paragraph("核心关系链路：meeting 1:N meeting_topic；meeting_topic N:M sys_department 通过 meeting_topic_department；meeting_topic N:M sys_user 通过 meeting_topic_attendee；meeting_sign_in 绑定 meeting、topic、user；meeting_notification 绑定 meeting、topic、receiver_user 或 receiver_department。"))

    nodes.append(paragraph("4.5 业务流程、接口与表调用", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["流程", "角色", "主要接口", "主要写表/读表", "结果"],
        [
            ["登录", "全部角色", "POST /api/auth/login, GET /api/auth/me", "读 sys_user + sys_department", "返回 token 和用户角色、部门、工号。"],
            ["会议列表", "管理员/非管理员", "GET /api/meetings", "管理员读 meeting；非管理员联合 meeting_topic、meeting_topic_attendee、meeting_topic_department、meeting_notification", "管理员看全部；普通用户看本人相关会议。"],
            ["创建/编辑会议", "会议管理员", "POST /api/meetings, PUT /api/meetings/{id}", "写 meeting、meeting_topic、meeting_topic_department", "形成 DRAFT 会议与议题结构。"],
            ["导入议题", "会议管理员", "POST /api/meetings/{id}/topics/import", "读 sys_department；写 meeting_topic、meeting_topic_department、meeting_uploaded_file", "从 .doc/.docx 解析议题，失败时提示手动新增。"],
            ["发布会议", "会议管理员", "POST /api/meetings/{id}/publish", "更新 meeting.status；写 meeting_notification；读 sys_user 秘书", "状态变为 PUBLISHED，涉及部门秘书收到通知记录。"],
            ["遴选参会人员", "邀请参会人员", "GET /api/meetings/{id}/selection-tasks, GET /api/users, POST /api/topics/{topicId}/attendees", "读 meeting_topic；读 sys_user；写 meeting_topic_attendee、meeting_notification", "参会名单同步到详情和管理员后台。"],
            ["会议详情", "全部相关角色", "GET /api/meetings/{id}/detail", "读 meeting、meeting_topic、meeting_topic_attendee、meeting_sign_in", "聚合基础信息、议题、参会人、结论、签到状态。"],
            ["签到", "参会人员", "POST /api/meetings/{id}/sign-qrcode, POST /api/sign-in", "meeting 写 sign_token；读 meeting_topic_attendee；写 meeting_sign_in；更新 meeting_topic.report_department_signed", "身份匹配则签到成功，签到看板实时可见。"],
            ["会议控制", "会议管理员", "POST /api/topics/{topicId}/start, POST /api/topics/{topicId}/end, PUT /api/topics/{topicId}/conclusion", "更新 meeting.status、meeting_topic.status/start_time/end_time/actual_minutes/conclusion", "议题计时、会议进程、会议结论落库。"],
            ["移动进程", "参会人员", "GET /api/meetings/{id}/mobile-progress", "读 meeting、meeting_topic", "仅返回当前议题序号和已结束议题时长。"]
        ],
        [1500, 1550, 2600, 3000, 2300]
    ))

    nodes.append(paragraph("4.6 角色使用过程串联", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(paragraph("会议管理员使用过程", style="Heading3", bold=True, size=12, color="1F4D78"))
    for text in [
        "登录系统：调用 POST /api/auth/login，读取 sys_user 识别 ADMIN。",
        "进入 PC 首页：调用 GET /api/meetings，管理员直接读取全部 meeting。",
        "创建会议：填写基础信息，保存时写入 meeting；如手动新增议题，同时写 meeting_topic 和 meeting_topic_department。",
        "导入议题：先保存会议获得 meeting.id，再上传 Word 文件；系统解析字段并写入议题表、部门关系表和上传记录表。",
        "发布会议：更新 meeting.status=PUBLISHED，按议题汇报部门和参会部门寻找秘书并写 meeting_notification。",
        "查看详情和参会人员：读取 meeting、topic、attendee、sign_in 聚合数据，展示部门、工号、姓名。",
        "会前生成签到二维码：更新 meeting.sign_token 和 sign_token_expires_at。",
        "会中控制：开始议题时置 meeting=IN_PROGRESS、topic=RUNNING；结束时写 end_time、actual_minutes；全部结束后 meeting=FINISHED。",
        "会后保存结论：更新 meeting_topic.conclusion，详情页和移动详情页读取展示。"
    ]:
        nodes.append(bullet(text))

    nodes.append(paragraph("邀请参会人员使用过程（部门秘书/科组长）", style="Heading3", bold=True, size=12, color="1F4D78"))
    for text in [
        "收到发布通知：通知记录位于 meeting_notification，后续可替换为企业微信/短信真实发送。",
        "登录后进入移动会议列表或分享详情：GET /api/meetings 按本人、通知、部门或参会关系过滤。",
        "查看可处理议题：SECRETARY 通过部门匹配 meeting_topic.report_department_id 和 meeting_topic_department；LEADER 通过 meeting_topic_attendee 中 attendee_type=LEADER 的记录匹配。",
        "选择人员：调用 GET /api/users，按本部门或角色筛选候选人，提交时写 meeting_topic_attendee。",
        "提交成功：系统给提交人写入 meeting_notification，管理员后台详情可立即看到名单。"
    ]:
        nodes.append(bullet(text))

    nodes.append(paragraph("参会人员使用过程", style="Heading3", bold=True, size=12, color="1F4D78"))
    for text in [
        "登录后查看本人相关会议：列表过滤依赖本人是否在 meeting_topic_attendee，或所属部门是否被议题涉及。",
        "进入移动详情：可查看会议基础信息、本人可见的参会人员列表和已录入结论。",
        "扫码签到：POST /api/sign-in 携带 token，系统先根据 meeting.sign_token 找会议，再检查本人是否在该会议任一议题的 meeting_topic_attendee 中。",
        "签到成功：为本人涉及的议题写 meeting_sign_in；若本人部门是议题汇报部门，则置 meeting_topic.report_department_signed=true。",
        "查看会议进程：移动端轮询 GET /api/meetings/{id}/mobile-progress，只展示当前议题序号和已结束时长。"
    ]:
        nodes.append(bullet(text))

    nodes.append(paragraph("4.7 漏洞与逻辑风险检查", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["风险点", "当前表现", "影响", "建议"],
        [
            ["接口级权限不完整", "create/update 有 ADMIN 校验；publish、import、reorder、sign-qrcode、start/end、conclusion 等接口当前主要依赖登录态。", "普通登录用户如果知道 ID，可能操作会议流程或结论。", "在服务层统一增加 assertAdmin 或细粒度权限校验，并补充接口测试。"],
            ["详情直达越权", "GET /api/meetings/{id}/detail 当前未校验当前用户是否与会议相关。", "分享链接仍需登录，但任意登录用户猜到会议 ID 可能查看详情。", "详情、dashboard、mobile-progress 都应复用“本人相关会议”校验。"],
            ["遴选提交越权", "submitAttendees 当前未强校验 SECRETARY/LEADER 是否有该 topic 的遴选资格。", "非责任人可能向任意议题提交名单。", "提交前校验 topic 与当前用户部门/leader 关系，管理员可作为例外。"],
            ["非管理员列表范围偏宽", "listMeetingsForUser 包含所属部门涉及议题，因此同部门普通参会人员可能看到尚未被本人选中的会议。", "会议可见范围可能超出“本人参与”的严格定义。", "按角色拆分过滤：SECRETARY 看部门任务，LEADER/PARTICIPANT 以 attendee 表为主。"],
            ["重复发布通知", "publish 未限制重复调用，可能重复写 notification。", "秘书收到多条重复通知，列表关系被重复强化。", "仅允许 DRAFT 发布，或发布前清理/去重通知。"],
            ["参会人数校验顺序", "submitAttendees 先按当前总数+本次提交数校验，再删除提交人旧名单。", "重新提交名单时可能误判超过最大人数。", "先计算本提交人原名单扣减后的净新增人数。"],
            ["二维码和签到安全", "sign_token 存在会议表，任何可调用二维码接口的人可刷新 token。", "可能导致会场二维码失效或被非管理员刷新。", "sign-qrcode 限定管理员；token 可增加随机盐、短有效期和刷新审计。"],
            ["外部通知与二次提醒未落地", "企业微信/短信为 mock；会前 2 天未提交提醒尚未实现。", "业务闭环依赖人工查看。", "增加定时任务扫描未提交 topic/department，并写通知或调用真实发送器。"],
            ["自动推进口径不一致", "PRD 提到结束后自动切换到下一个议题；当前服务只结束当前议题，不自动开始下一项。", "管理员仍需手动点击下一项开始。", "明确产品口径：保留手动开始，或实现 end 后自动查找下一 WAITING 并 start。"],
            ["审计日志不足", "当前只记录通知、上传、签到，未记录管理员关键操作审计。", "出现争议时难以追溯谁发布、谁改结论、谁刷新二维码。", "新增 operation_log，记录 actor、action、target、before/after、ip、时间。"]
        ],
        [1900, 2900, 2300, 3200]
    ))

    nodes.append(paragraph("4.8 建议补充的验收用例", style="Heading2", bold=True, size=13, color="2E74B5"))
    for text in [
        "权限：非管理员调用 publish、import、start/end、conclusion、sign-qrcode 应返回无权限。",
        "数据可见性：非相关用户直接访问 /api/meetings/{id}/detail、dashboard、mobile-progress 应被拒绝。",
        "遴选：秘书只能处理本部门相关议题，科组长只能处理自己被秘书选中的议题。",
        "重复发布：同一会议重复发布不应生成重复通知。",
        "名单重提：同一提交人修改名单时应按净新增人数计算最大参会人数。",
        "签到：未入名单、重复签到、过期二维码、刷新二维码后的旧 token 均应有明确提示。",
        "会议进程：同一会议不能同时存在两个 RUNNING 议题；全部 FINISHED 后会议状态必须为 FINISHED。",
        "文档导入：非 Word、空文件、超过 50MB、无法识别结构、部门名未匹配均应有可理解提示并允许手动新增。"
    ]:
        nodes.append(bullet(text))

    nodes.append(paragraph("4.9 后续数据库增强建议", style="Heading2", bold=True, size=13, color="2E74B5"))
    nodes.append(table(
        ["建议表/字段", "目的", "说明"],
        [
            ["operation_log", "关键操作审计", "记录发布、导入、开始/结束议题、修改结论、刷新二维码、名单提交等行为。"],
            ["meeting_topic_attendee.confirm_status", "名单确认状态", "区分草稿、已提交、管理员确认，支持撤回和二次调整。"],
            ["meeting_notification.business_key", "通知幂等", "按 meeting_id + topic_id + receiver_user_id + notice_type 去重。"],
            ["meeting_topic_department.selection_deadline", "催办提醒", "支持会前 2 天二次提醒和逾期统计。"],
            ["meeting_sign_in.sign_source_detail", "签到来源追踪", "记录企业微信 userId、扫码设备、IP 或人工补签原因。"]
        ],
        [2600, 2400, 4360]
    ))
    return nodes


def main():
    src = Path("PRD-source.docx")
    out = Path("PRD-updated.docx")
    with zipfile.ZipFile(src, "r") as zin:
        files = {name: zin.read(name) for name in zin.namelist()}
    root = ET.fromstring(files["word/document.xml"])
    body = root.find(qn("body"))
    sect = body.find(qn("sectPr"))
    sect_copy = copy.deepcopy(sect) if sect is not None else None
    if sect is not None:
        body.remove(sect)
    for node in build_appendix():
        body.append(node)
    if sect_copy is not None:
        body.append(sect_copy)
    files["word/document.xml"] = ET.tostring(root, encoding="utf-8", xml_declaration=True)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
        for name, data in files.items():
            zout.writestr(name, data)
    print(out.resolve())


if __name__ == "__main__":
    main()
