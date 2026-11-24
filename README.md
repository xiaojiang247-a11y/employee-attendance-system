# 企业员工考勤管理系统

> 毕业设计题目：**《基于Java的企业员工考勤管理系统设计与实现》**

一套可直接运行的单体式企业考勤系统。同一份 jar 按端口启动三个角色隔离的子系统，覆盖员工打卡、管理员业务处理、超级管理员账户与权限治理；时间统一走北京时间，打卡必须在管理员设定的固定地点范围内完成。

前端零 CDN 依赖，断网也能完整渲染。

---

## 一、功能特性

**三端权限隔离**

| 端口 | 子系统 | 登录后落地页 | 账号来源 |
|---|---|---|---|
| 8081 | 超级管理员系统 | `/super/dashboard` | 首次启动自动创建唯一 `superadmin` |
| 8082 | 管理员系统 | `/dashboard` | 超管在 `/super/admins` 创建 |
| 8083 | 员工系统 | `/my/home` | 管理员在 `/admin/system/users` 创建 |

`--server.port` 与 `--app.port-role` 绑定，登录时由端口守卫校验角色，账号角色与端口不匹配一律拒绝。系统**不预置任何管理员、员工账号和业务数据**，全部由使用者自行录入。

**考勤业务**

- 打卡规则：09:00 上班、09:02 后计迟到、17:59 前计早退、18:00 下班；迟到/早退分钟向上取整。
- 固定地点围栏：GPS（Haversine 半径）、IP 白名单、WiFi SSID 三类校验；未配置地点时拒绝打卡；超管设置的地点覆盖管理员设置。
- 请假、加班、出差、补卡、考勤申诉、多级审批、假期额度、排班与考勤组、工作日历、统计报表与图表。
- 账户管理：账户列表、启用/停用、改密/重置、角色绑定、账户操作流水 `sys_account_log`、登录审计。
- RBAC：角色、菜单、角色-菜单绑定、URL 分段隔离、菜单访问拦截、数据归属断言。

**工程实现**

- 时区统一：JVM 默认时区、Hikari `SET time_zone='+08:00'`、Jackson、业务 `AppClock`、前端时钟全部对齐 `Asia/Shanghai`。
- 安全：Spring Security 6、CSRF、会话注册表、密码 BCrypt、错误页/403 页、业务异常回源提示。
- 响应式：登录页品牌横幅在浏览器缩放到 200% 仍保持可见；内页侧栏在窄视口折叠；宽表可横向滑动。
- 自绘 SVG 图表，不依赖 ECharts 等外部库。

---

## 二、技术栈

| 层 | 选型 |
|---|---|
| 语言 / 运行时 | Java 21 |
| 框架 | Spring Boot 3.3.5、Spring MVC、Spring Security 6 |
| 模板 | Thymeleaf 3.1 |
| 持久层 | MyBatis + MySQL 8（HikariCP） |
| 前端 | 原生 HTML/CSS/JS，无构建、无 CDN |
| 构建 | Maven |
| 时间 | `Asia/Shanghai`（UTC+8） |

---

## 三、目录结构

```
├── src/main/java/com/example/attendance/
│   ├── config/        SecurityConfig、PortRoleGuard、DataInitializer、
│   │                  GlobalExceptionHandler、WebConfig、LayoutInterceptor
│   ├── controller/    三端控制器
│   ├── service/       业务服务与权限断言
│   ├── mapper/        MyBatis 映射
│   ├── entity/        实体
│   └── util/          AppClock、PunchRule、Page
├── src/main/resources/
│   ├── templates/     Thymeleaf 页面（49 个）
│   ├── static/        css / js / favicon.svg / favicon.ico
│   └── application.yml
├── sql/
│   ├── schema_v1.sql       从零建库基线（结构 + 系统字典，推荐）
│   ├── schema_v2.sql       历史增量升级脚本（幂等）
│   ├── schema_v3.sql       三级权限隔离升级
│   ├── schema_v4.sql       三端口边界 + 打卡地点覆盖
│   ├── clean_demo_data.sql 清空演示业务数据
│   └── demo_data.sql       可选演示数据（默认不导入）
├── test/              回归验证脚本
├── start-all.bat      一次拉起三个端口
├── start-super.bat / start-admin.bat / start-employee.bat
├── application-local.yml.example   本机私有配置模板
├── AUDIT_REPORT.md    全面代码审计报告
└── pom.xml
```

---

## 四、快速开始

### 1. 环境

JDK 21、Maven、MySQL 8（服务已启动）。

### 2. 建库

从零部署只需执行一份基线脚本：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < sql/schema_v1.sql
```

该脚本建 `attendance_db` 库、27 张表，并写入角色、菜单、角色菜单绑定、部门、班次、假别、工作日历、考勤组等系统字典；**不含任何账号、员工档案、考勤流水或口令**。

`schema_v2/v3/v4.sql` 是从旧版本库升级用的幂等增量脚本，新库无需执行。

### 3. 配置本机私有参数

```bash
cp application-local.yml.example application-local.yml
```

编辑 `application-local.yml`，把 `CHANGE_ME_DB_PASSWORD` 改成你的 MySQL 口令，把 `CHANGE_ME_SUPER_PASSWORD` 改成首次启动要用的超管初始口令。该文件已被 `.gitignore` 排除，不会进入仓库。

也可以完全不用这个文件，改用环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/attendance_db?...` | JDBC 连接串 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | 空 | 数据库口令，必填 |
| `SUPER_ADMIN_PASSWORD` | `ChangeMe@2026` | 仅首次建库（库中无 `superadmin`）时生效 |

环境变量优先级高于 `application-local.yml`。

### 4. 编译

```bash
mvn -DskipTests package
```

产物：`target/attendance-system-1.0.0.jar`（约 30 MB，Spring Boot 可执行 fat jar）。

### 5. 启动

Windows 下双击 `start-all.bat`，一次拉起三个端口。或分别执行：

```bash
java -Dfile.encoding=UTF-8 -jar target/attendance-system-1.0.0.jar --server.port=8081 --app.port-role=SUPER_ADMIN --app.port-role-label=超级管理员系统
java -Dfile.encoding=UTF-8 -jar target/attendance-system-1.0.0.jar --server.port=8082 --app.port-role=ADMIN --app.port-role-label=管理员系统
java -Dfile.encoding=UTF-8 -jar target/attendance-system-1.0.0.jar --server.port=8083 --app.port-role=EMPLOYEE --app.port-role-label=员工系统
```

### 6. 登录

打开 http://localhost:8081/login ，用 `superadmin` + 你在 `application-local.yml` 里设置的初始口令登录超级管理员系统。

首次登录后按以下顺序补齐账号：

1. 超管端 `/super/admins` 创建管理员账号；
2. 管理员登录 8082，在 `/admin/system/users` 创建员工账号与员工档案；
3. 员工登录 8083，在管理员设定的固定地点范围内打卡。

8082 / 8083 的登录页在建好对应账号前可以打开，但没有账号可登录。

---

## 五、打卡与围栏规则

| 规则 | 取值 |
|---|---|
| 上班时间 | 09:00 |
| 迟到判定 | 09:02 之后签到 |
| 早退判定 | 17:59 之前签退 |
| 下班时间 | 18:00 |
| 迟到/早退分钟 | 向上取整 |
| 地点校验 | GPS 半径 / IP 白名单 / WiFi SSID |
| 未配置地点 | 拒绝打卡 |
| 地点优先级 | 超管默认地点 > 管理员设置 |
| 时间口径 | `Asia/Shanghai`，前端时钟跨时区仍显示北京时间 |

地点在管理员端 `/admin/att/locations` 设置，超管端 `/super/locations` 可设置覆盖默认地点。

---

## 六、回归验证

`test/` 下的脚本用于验证围栏、越权、异常页、补卡边界等修复是否生效。运行前需要：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库口令
export SUPER_PASS=你的超管口令
export MYSQL_BIN=mysql          # 或 mysql.exe 的完整路径
export TMPDIR=/tmp              # 会话快照目录
```

脚本默认连接 8091/8092/8093 三个回归端口（`verify_fixes.sh`）或 8081/8082/8083（`fence_check.sh`），需先按第四节的方式用对应端口启动实例。脚本会自行创建并清理测试账号、地点与排班。

`AUDIT_REPORT.md` 记录了 10 个已确认缺陷的复现条件、根因、影响范围、最小修改方案与验证结果。

---

## 七、已知设计取舍

- `sys_user.plain_password` 保存明文口令，目的是让管理员在后台查看/告知初始密码；若用于正式部署，应移除该列及账户页中的展示，改为一次性临时口令或邮件/短信下发。
- `demo_data.sql` 是可选演示数据（含演示姓名、工号与统一初始口令 `123456`），默认不执行；`clean_demo_data.sql` 用于清空演示业务数据。
- 菜单访问拦截器的菜单缓存为 30 秒，修改角色菜单绑定后最多 30 秒生效。
- 三个端口使用独立内存会话，跨端口访问业务页会跳转登录页，隔离成立。

---

## 八、许可证与贡献

本项目采用 [MIT License](LICENSE)。若需把版权人改为你的名字，编辑 `LICENSE` 第 3 行即可。

欢迎提交 Issue 与 Pull Request。提交前请确认：不含任何真实口令、不含本机绝对路径、新增 SQL 幂等、改动经过 `mvn -DskipTests package` 验证。
