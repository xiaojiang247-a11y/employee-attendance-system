# 员工考勤管理系统 · 全面代码审计报告

- 审计时间：2026-11-22（北京时间，Asia/Shanghai）
- 审计对象：`员工系统/` Spring Boot 3.3.5 + MyBatis + Thymeleaf + Spring Security 主工程（V4 三端口角色端）
- 审计范围：95 个 Java 源文件、49 个 Thymeleaf 模板、全部 Mapper 注解 SQL、`application.yml`、前端 `app.js`
- 审计重点：功能性错误、边界条件、异常处理、数据流、交互逻辑、越权与核心约束绕过
- 审计方法：源码逐块审查 + 数据库 SQL 复现 + 三实例黑盒 HTTP 复现 + 离线编译验证 + 补丁干跑重建比对

## 环境事件（影响验证方式，非代码缺陷）

审计期间 `E:` 可移动磁盘掉线（卷标曾变为 ESD-USB，`vol E:` 报「系统找不到指定的路径」）。原 8081/8082/8083 三个实例虽仍在内存运行，但模板资源不可读，`/403` 等页面渲染 500，黑盒验证失真。

处理方式：从会话记录重放 V2/V3 生成脚本 + 叠加 V4 写改，在 C 盘固定盘重建完整源码基线 `项目根目录`（95 java / 49 模板，含 `pom.xml` 与 `application.yml`），修复两处快照失真后离线编译通过。修复版三实例改在 **8091 / 8092 / 8093** 运行验证，模板完整、`/403`=200。

> 原项目位于 E 盘，本次修复先落在 C 盘副本并完整验证；E 盘恢复后按第九节把补丁应用回原项目即可。

---

## 一、结论摘要

共确认 **10 个缺陷**（1 个 P0、2 个 P1、6 个 P2、1 个 P3），已全部按最小修改原则修复并验证；另复核排除 3 项误报、列出 1 项已知限制。

| 编号 | 级别 | 缺陷 | 状态 |
|---|---|---|---|
| BUG-01 | 严重 P0 | 伪造 `X-Forwarded-For` 可绕过固定地点打卡围栏 | 已修复 + 黑盒复现验证 |
| BUG-02 | 中等 P1 | 超管接管/覆盖管理员地点后 `findSuperDefault` 命中不到，覆盖语义失效 | 已修复 + SQL/黑盒验证 |
| BUG-03 | 中等 P1 | 角色双来源（`sys_user.role` 与 `sys_user_role`）导致员工端口可获超管权限 | 已修复 + 黑盒验证 |
| BUG-04 | 中等 P2 | 员工端申请参数缺校验，非法日期/负时长/倒挂日期进 500 或脏数据 | 已修复 + 黑盒验证 |
| BUG-05 | 中等 P2 | 500 错误页渲染成 HTTP 200 | 已修复 + 黑盒验证 |
| BUG-06 | 中等 P2 | 异常申诉复核把缺卡/旷工状态洗成 `NORMAL` | 已修复 + 编译/代码复核 |
| BUG-07 | 中等 P2 | 请假天数计算与「标记考勤」使用两套工作日口径 | 已修复 + 编译/代码复核 |
| BUG-08 | 中等 P2 | 地点操作未校验目标存在性，删不存在的 id 仍记 `SUCCESS` | 已修复 + 编译/代码复核 |
| BUG-11 | 中等 P2 | `BizException` 无 Referer 时跳 `/dashboard`，员工/超管端 403 且提示丢失 | 已修复 + 黑盒验证 |
| BUG-10 | 轻微 P3 | 补卡可制造签退早于签到的非法时间对 | 已修复 + 编译/代码复核 |

---

## 二、严重（P0）

### BUG-01 伪造 `X-Forwarded-For` 绕过固定地点打卡围栏

**涉及文件**
- `controller/EmployeePortalController.java:83` `clientIp()`
- `service/PunchLocationService.java:56-91` `verify()`
- `service/AccountService.java:234` 审计日志取 IP

**复现条件（已实测）**
1. 管理员配置一个地点：北京坐标、半径 100 米、`allow_ip='10.99.99.99'`、启用、默认。
2. 员工在上海，无 GPS 坐标上报、TCP 对端为 `127.0.0.1`。
3. `POST /my/check-in` 带 `X-Forwarded-For: 10.99.99.99`。
4. 结果：`attendance_record` 落库成功（`id=1740`，`check_in_ip=10.99.99.99`），围栏完全失效。

**根本原因**
- `clientIp()` 优先取 `X-Forwarded-For` 首段，该头由客户端任意构造；服务直连、无可信反向代理，唯一可信值是 TCP 对端 `getRemoteAddr()`。
- `verify()` 原本是「GPS / IP / WiFi **任一命中即放行**」的 OR 逻辑：IP 白名单单独命中就返回 `Result(..., "IP")`。而 GPS 经纬度与 WiFi SSID 都是浏览器表单上报值，同样可任意伪造（浏览器本身无法读取 SSID）。
- 只填名称、三因子未配置的地点虽被跳过，但只要有一项可伪造因子即可放行。
- 审计日志 IP 同样取自 XFF，导致操作记录被污染、事后无法追溯真实来源。

**影响范围**
- 用户明确要求的核心约束「员工必须在管理员设定的固定地点范围内打卡，别的位置打不了」完全失效；任意外网位置可打卡。
- 影响签到/签退、迟到早退核算、月度汇总、异常申诉全链路数据可信度。
- 账户/地点操作日志的 IP 字段不可信。

**最小修改方案（22 处补丁中的 P1a/P1b/P1b2/P1c）**
1. `clientIp()` 只返回 `req.getRemoteAddr()`；`AccountService` 审计日志同源。
2. `verify()` 把 IP 白名单升级为**硬门禁**：地点一旦配置 `allow_ip`，必须先命中才继续评估（`hasIpRule && !ipOk` → `continue`）；`ipOk` 才可作为放行依据。
3. 移除 WiFi SSID 放行分支（历史 `WIFI` 匹配类型仍由 `matchLabel` 兼容展示）。
4. 客户端坐标增加 `validLatLng()` 范围校验（-90~90 / -180~180），避免非法值写进流水或让距离失真。

**验证方式**
- `test/verify_fixes.sh` 第 2 步：无 XFF 与伪造 XFF 两次签到均 `HTTP=302 → /my/home`、`DB=0`、flash 为「打卡失败：当前位置不在允许的打卡范围内…」。
- 第 3 步不回归：错误坐标（距 1,068,927 米）拒绝 `DB=0`；正确上海坐标成功 `DB=1` 且提示「上班打卡成功…」。
- `test/precise_probe.sh` 输出完整响应链与 flash 文案。

---

## 三、中等（P1）

### BUG-02 超管接管/覆盖管理员地点后覆盖语义失效

**涉及文件**：`controller/SuperLocationController.java:71,92`、`mapper/PunchLocationMapper.java:73,78-93`、`service/PunchLocationService.java:93` `candidatesFor()`

**复现条件（已用 SQL 复现）**
管理员创建地点 A（`created_by_role='ADMIN'`, `is_default=1`）→ 超管执行 `/super/locations/default/{A}` → `is_default` 变了，但 `created_by_role` 仍是 `ADMIN` →
`SELECT COUNT(*) FROM punch_location WHERE enabled=1 AND is_default=1 AND created_by_role='SUPER_ADMIN'` = **0**。

**根本原因**：`setDefault(id)` 只改 `is_default`；超管覆盖修改走 `locationMapper.update(l)`，不改写设置人层级。而 `candidatesFor()` 一旦查到「超管默认地点」就只返回该地点、让管理员其它地点失效，`findSuperDefault()` 却按 `created_by_role='SUPER_ADMIN'` 过滤。两处语义不一致，超管的「设为默认」对管理员创建的地点等于空操作；管理员还能反向改回去（层级保护也依赖同一字段）。

**影响范围**：用户明确要求的「超级管理员可修改或覆盖管理员设置的打卡地点」在该路径下失效；超管以为已统一地点，实际仍按管理员配置放行。

**最小修改方案（P2a/P2b/P2c）**
- 新增 `takeover(id, actor)`：补写 `created_by_role='SUPER_ADMIN'` 与接管人、`update_time=NOW()`。
- 新增 `setSuperDefault(id)`：设默认的同时把该行标记为超管接管；`setDefault` 补 `WHERE id=#{id} OR is_default=1` 限定影响行。
- 超管覆盖修改 `update(l)` 后调用 `takeover()`；设默认改走 `setSuperDefault()`。

**验证方式**：`test/verify_fixes.sh` 第 4 步 —— 接管前 `ADMIN` → 超管设默认 → 接管后 `SUPER_ADMIN`、`findSuperDefault` 命中数 = 1、管理员停用被拒并提示「该地点由超级管理员设置，管理员不能修改」、`enabled` 仍为 1。

### BUG-03 角色双来源导致员工端口跨端越权

**涉及文件**：`config/CustomUserDetailsService.java:32-34`、`config/PortRoleGuard.java`、`config/RoleBasedSuccessHandler.java`、`config/SecurityConfig.java`

**复现条件**：构造账号 `sys_user.role='EMPLOYEE'`、`role_level='EMPLOYEE'`，但 `sys_user_role` 绑定 `SUPER_ADMIN`（脏数据、历史迁移、误授权或后台越权写入都能产生）。登录 8083 员工端口后，authorities 同时含 `ROLE_EMPLOYEE` 与 `ROLE_SUPER_ADMIN`。

**根本原因**：`CustomUserDetailsService` 原本把 `sys_user.role` **和** `findRoleCodes()`（`sys_user_role`）都加成 `ROLE_`。`PortRoleGuard` 按「第一个 `ROLE_`」判定端口（EMPLOYEE → 放行），而 `SecurityConfig.hasRole("SUPER_ADMIN")` 基于全部 authorities（通过）。两处角色集合不同源，同一份认证在两个环节被解读成不同角色。

**影响范围**：员工端口可访问 `/super/**`（管理员账号、员工账号、权限边界、账户日志、地点设置），构成完整越权面。

**最小修改方案（P3）**：主角色只取 `sys_user.role` 单一来源，不再并入 `findRoleCodes()` 的 `ROLE_`；`findPermCodes()` 的菜单权限码保留，不影响菜单授权。

**验证方式**：`test/verify_fixes.sh` 第 5 步 —— 错配账号登录 8093 后 `/super/admins`、`/super/employees`、`/admin/system/users` 全部 403；第 7 步正常三端权限隔离无回归。

---

## 四、中等（P2）

### BUG-04 员工端申请参数缺校验

**涉及文件**：`controller/EmployeePortalController.java:203,229,279,306,376`

**复现条件**：请假/出差提交 `endDate` 早于 `startDate`、日期字符串非 `yyyy-MM-dd`、加班 `hours=-5`、补卡 `makeupTime` 非 `HH:mm`。

**根本原因**：直接使用 `LocalDate.parse()` / `LocalDateTime.parse(missDate + "T" + makeupTime)`，格式错误抛 `DateTimeParseException` 冒到 500 页；日期倒挂、负时长、超 24 小时、补卡时间格式（`H:mm` 会被拼成 `T5:30`）均无业务校验，脏数据可直接入库。

**影响范围**：员工端四类申请（请假/加班/出差/补卡）体验与数据质量；异常走到 500 错误页，用户丢失已填内容。

**最小修改方案（P5a-P5e）**：新增 `reqDate()` 把格式异常转成 `BizException` 业务提示；请假/出差加「结束不早于开始」；加班时长限制 `0 < hours <= 24`；补卡用 `LocalDate` + `LocalTime.parse` 结构化组装并校验 `HH:mm`。

**验证方式**：`test/verify_fixes.sh` 第 6 步 —— 出差日期倒挂提示「结束日期不能早于开始日期」、加班负时长提示「加班时长必须在 0~24 小时之间」，均 302 回源而非 500。

### BUG-05 500 错误页渲染成 HTTP 200

**涉及文件**：`config/GlobalExceptionHandler.java:40-44`

**复现条件**：任意未捕获异常（如 `POST /my/appeal` 传 `recordId=abc` 触发类型绑定异常）。

**根本原因**：`@ExceptionHandler(Exception.class)` 返回 `error/500` 视图名，但视图渲染不会自动设置响应状态码。上一轮虽补齐了 `error/500.html` 模板（解决了「二次 TemplateInputException → 连接被关闭」），状态码仍是 200。

**影响范围**：监控、健康检查、前端 fetch 都无法区分服务端错误与正常响应；错误被静默吞掉。

**最小修改方案（P4a/P4b）**：注入 `HttpServletResponse`，`response.setStatus(SC_INTERNAL_SERVER_ERROR)`。

**验证方式**：`test/verify_fixes.sh` 第 6 步 —— 非法 Long 参数 `HTTP=500`、页面显示「系统处理请求时出错」。

### BUG-06 申诉复核把缺卡洗成 `NORMAL`

**涉及文件**：`service/AttendanceService.java:231-255` `correctAppeal()`

**复现条件**：员工漏打卡 → 记录状态 `ABSENT` / `MISSING_IN` / `MISSING_OUT` → 提交异常申诉 → 审批通过。

**根本原因**：原实现无条件 `r.setStatus("NORMAL")` 并清空迟到/早退分钟，等于凭空补造打卡事实。

**影响范围**：月度汇总把漏打卡算出勤，考勤统计与薪资依据失真。

**最小修改方案（P6）**：`correctAppeal()` 只撤销迟到/早退判定；`missing` 状态保留；仅在签到签退齐全且有班次时重算工时。

**验证方式**：离线编译通过 + 代码复核；该项为状态机内部逻辑，未做黑盒端到端复现（需构造审批单据，标注为「编译/代码复核」）。

### BUG-07 请假天数与标记考勤使用两套工作日口径

**涉及文件**：`service/LeaveService.java:54,60,121`

**复现条件**：请假区间包含「周末调休上班日」（`holiday_calendar.day_type='WORKDAY'`）或法定节假日。

**根本原因**：`calcDays()` 把调休上班日计入请假天数，但 `onApproved()` 里用 `if (d.getDayOfWeek().getValue() >= 6) continue;` 直接跳过所有周末（含调休上班日），也未查节假日表标记。额度扣减天数与写入考勤记录的天数不一致。

**影响范围**：假期额度扣减与考勤标记不匹配，跨调休的请假单出现「扣了额度但没标记」或反之。

**最小修改方案（P7a/P7b）**：抽出 `isWorkday(d, restDays, makeUpWorkdays)`（调休上班日计入、周末与节假日剔除），`calcDays()` 与 `onApproved()` 共用；`onApproved()` 预加载区间日历，避免逐日查库。

**验证方式**：离线编译通过 + 代码复核。

### BUG-08 地点操作未校验目标存在性

**涉及文件**：`controller/LocationController.java:110-111`、`controller/SuperLocationController.java:112-113`

**复现条件**：对已删除或不存在的地点 id 发起 `toggle` / `delete` / `default` / `save`（并发下另一管理员已删除也会触发）。

**根本原因**：`mutate()` 取 `findById(id)` 后未判空就执行 `op.run()`；Mapper 的 `update`/`delete` 影响 0 行不报错，仍记 `result=SUCCESS` 日志并提示「操作成功」。

**影响范围**：操作日志出现虚假成功记录，审计不可信；并发删除下用户被误导。

**最小修改方案（P8a/P8b）**：`mutate()` 开头判空 → `writeFailure(...)` + flash 错误提示 + 重定向回列表。

**验证方式**：离线编译通过 + 代码复核。

### BUG-11 业务异常无 Referer 时跳到无权访问页

**涉及文件**：`config/GlobalExceptionHandler.java:22-37` `handleBiz()`

**复现条件**：员工端 POST 触发 `BizException` 且请求不带 `Referer`（API 调用、隐私策略剥离、部分客户端），例如 `POST /my/trip/apply` 日期倒挂。

**根本原因**：`handleBiz()` 有 Referer 时回源，无 Referer 时统一 `redirect:/dashboard`；`/dashboard` 在 `SecurityConfig` 只允许 `SUPER_ADMIN/ADMIN/HR/MANAGER`，员工与超管端访问即 403，一次性 flash 提示随之丢失。

**影响范围**：非浏览器场景的业务校验错误显示 403 且无提示；超管端同样受影响。

**最小修改方案（P10）**：无 Referer 时按请求 URI 前缀回源：`/my/**` → `/my/home`，`/super/**` → `/super/dashboard`，其余 `/dashboard`。

**验证方式**：`test/verify_fixes.sh` 第 6 步「无Referer」行 —— `LOC=/my/home`、提示「结束日期不能早于开始日期」；有 Referer 对照组同样正确。

---

## 五、轻微（P3）

### BUG-10 补卡可制造签退早于签到的非法时间对

**涉及文件**：`service/AttendanceService.java:217-224` `applyMakeup()`

**复现条件**：已有签到 09:00，补卡申请「签退 08:30」并审批通过。

**根本原因**：`applyMakeup()` 直接 `setCheckOutTime(makeupTime)`，未与已有签到时间比较；随后 `recalc()` 计算工时的时间差为负/跨日，产生异常工时与状态。

**影响范围**：单条考勤记录工时与状态失真，影响月度汇总；属低频边界。

**最小修改方案（P9）**：写入后校验 `checkOutTime.isBefore(checkInTime)` → 抛 `BizException("补卡失败：签退时间不能早于签到时间")`，事务回滚。

**验证方式**：离线编译通过 + 代码复核。

---

## 六、已复核排除（避免误报）

| 项 | 复核结论 |
|---|---|
| 审批单据部门数据隔离 | `ApprovalMapper.findPending()` 与各 `findById` 通过 `JOIN employee` 回填 `dept_id`，过滤有效，非缺陷 |
| `util/Page.of()` 越界/空列表 | 已有 `size<=0` 兜底、`page` clamp 到 `[1,totalPages]`、`from>=size` 返回空列表，非缺陷 |
| 重复签到 | `AttendanceService.java:97-99` 已有 `if (r != null && r.getCheckInTime() != null) return "今日已签到…"`，非缺陷 |

## 七、已知限制（本次不修改）

- `config/MenuAccessInterceptor.java:27` 菜单表缓存 TTL 30 秒：撤销某菜单授权后，最长 30 秒内直接输 URL 仍可访问。属于性能与实时性权衡；若要求即时失效，应在角色/菜单授权变更处主动清缓存。本次不改，避免引入额外的缓存一致性逻辑。
- 打卡规则按用户规定固定为 9:00 / 9:02 / 17:59 / 18:00，未设「签到截止时间」：20:27 签到仍算迟到 688 分钟（从 9:00 起算向上取整），符合既定口径，不是缺陷。

---

## 八、修复落地与回归验证

**补丁**：10 组缺陷共 22 处唯一命中替换，脚本 `test/apply_audit_patches.py`；等价 unified diff `test/audit_fixes.patch`（360 行，10 个文件，已 `patch --dry-run -p1` 通过）。

**不引入新问题的证据链**
1. 恢复源码基线离线编译 `mvn -o -q clean package -DskipTests` → `BUILD SUCCESS`，30.6 MB jar。
2. 补丁后重新编译 → `PATCHED_BUILD_EXIT=0`，30,669,959 字节 jar。
3. **补丁可重建性**：把脚本指向「打补丁前的原始基线副本」干跑 → `APPLIED=22/22`，产物与当前编译版 `src` 逐文件比对 `DIFF_EXIT=0`（无差异）。说明脚本可直接用于 E 盘原始项目。
4. 修复版三实例 8091/8092/8093 启动，登录页标题分别为「超级管理员系统 · 登录」「管理员系统 · 登录」「员工系统 · 登录」，`/403` 模板可读（200）。

**综合黑盒回归（`test/verify_fixes.sh`，8 组全通过）**

| 组 | 结果 |
|---|---|
| 1 三端登录分流 | 8091→`/super/dashboard`、8092→`/dashboard`、8093→`/my/home` |
| 2 BUG-01 | 无 XFF / 伪造 XFF 均 302、DB=0、围栏拒绝提示；错坐标拒绝、对坐标成功（不回归） |
| 3 GPS 正常打卡 | 错坐标 DB=0、对坐标 DB=1 且提示成功 |
| 4 BUG-02 | 接管后 `created_by_role=SUPER_ADMIN`、`findSuperDefault`=1、管理员停用被拒、`enabled=1` |
| 5 BUG-03 | 错配账号三个越权页面全 403 |
| 6 BUG-04/05/11 | 有/无 Referer 均 302 回 `/my/home` 且提示正确；非法 Long 参数 HTTP=500 |
| 7 权限隔离不回归 | 员工 `/super/**`、`/admin/**` 403；管理员 `/super/admins` 403；超管 `/super/dashboard` 200、`/my/home` 403；员工 `/my/home` 200 |
| 8 数据清理 | 账户 4、地点 0、今日排班 0、验证账号 `admin02`/`probe_emp` 与孤儿绑定全部清理 |

---

## 九、交付物与把修复应用回原项目

| 文件 | 说明 |
|---|---|
| `项目根目录/` | C 盘固定盘上的完整修复版工程（已编译、已验证） |
| `target/attendance-system-1.0.0.jar` | 修复版可执行 jar |
| `员工系统/test/apply_audit_patches.py` | 22 处补丁脚本（推荐；放在任意项目 `test/` 下自动定位，或显式传目标路径） |
| `员工系统/test/audit_fixes.patch` | 等价 unified diff（CRLF 行尾，`patch -p1` 干跑通过） |
| `员工系统/test/verify_fixes.sh` | 综合黑盒回归脚本（8 组） |
| `员工系统/test/precise_probe.sh` | 精确响应链探针（flash 文案级验证） |
| `员工系统/test/xff_probe.sh` | P0 原始复现脚本 |
| `员工系统/test/fence_check.sh` | 上一轮围栏闭环验证脚本 |
| `员工系统/AUDIT_REPORT.md` | 本报告 |

**E 盘恢复后的应用方式（二选一）**

```bash
# 方式 A：Python 脚本（推荐，行尾无关，已干跑验证 22/22）
# 把 test/ 目录复制到 E 盘项目 员工系统/ 下，脚本会自动按 <项目>/test/ 定位源码
cd "."
python3 test/apply_audit_patches.py
# 也可显式指定目标源码目录：
# python3 test/apply_audit_patches.py "src/main/java/com/example/attendance"

# 方式 B：unified diff（在项目根执行，目标文件为 CRLF 行尾）
cd ".."
patch -p1 < "员工系统/test/audit_fixes.patch"

# 然后重新打包并重启三端口
mvn -o -q clean package -DskipTests
员工系统/start-all.bat
```

应用补丁后先跑 `员工系统/test/verify_fixes.sh`（脚本内端口为 8091/8092/8093，
如按 `start-all.bat` 启动的是 8081/8082/8083，把脚本端口替换后再跑）。

> 注意：`mvn spring-boot:run` 在中文路径下会因插件临时 `.argfile` 编码问题报 `ClassNotFoundException`，统一用 `java -jar` / `.bat`。建议把项目从可移动磁盘复制到本机固定盘，避免再次掉线导致源文件丢失与资源读取失败。
