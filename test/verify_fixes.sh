#!/usr/bin/env bash
# 修复版回归验证（8091 超管 / 8092 管理员 / 8093 员工，共用 attendance_db）
# 注意：curl 不能同时用 -X POST 与 -L —— 跟随 302 时会继续用 POST 打 GET-only 页面，
# 得到假的 403。这里不跟随重定向，用 302 Location + 落库数据作为硬证据。
set -u
: "${DB_USERNAME:=root}"
: "${DB_PASSWORD:=}"
: "${SUPER_PASS:=ChangeMe@2026}"
: "${MYSQL_BIN:=mysql}"
: "${TMPDIR:=/tmp}"
MYSQL="$MYSQL_BIN"
sql() { "$MYSQL" -u"$DB_USERNAME" -p"$DB_PASSWORD" --default-character-set=utf8mb4 -e "USE attendance_db; $1" 2>&1 | grep -v "Warning"; }
csrf() { grep -o 'name="_csrf"[^>]*value="[^"]*"' "$1" | head -1 | sed 's/.*value="\([^"]*\)".*/\1/'; }
D=$(date +%F)
TMP=""${TMPDIR}/attendance-test"/fix"
rm -rf "$TMP"; mkdir -p "$TMP"

login() { # port user pass jar -> 打印 Location
  local p=$1 u=$2 pw=$3 j=$4
  curl -s -c "$j" --max-time 10 "http://localhost:$p/login" -o "$j.lg"
  local t; t=$(csrf "$j.lg")
  curl -s -b "$j" -c "$j" -D "$j.hdr" -o /dev/null --max-time 10 \
    --data-urlencode "username=$u" --data-urlencode "password=$pw" --data-urlencode "_csrf=$t" \
    "http://localhost:$p/login"
  grep -i '^Location:' "$j.hdr" | tr -d '\r' | sed 's/^Location: //'
}
token() { # port jar -> 从所属端首页取会话 CSRF
  local p=$1 j=$2 home=/my/home
  [ "$p" = "8091" ] && home=/super/dashboard
  [ "$p" = "8092" ] && home=/dashboard
  curl -s -b "$j" -c "$j" -o "$j.tok" --max-time 10 "http://localhost:$p$home" >/dev/null
  csrf "$j.tok"
}
post() { # port jar path data... -> 输出 HTTP 状态码，写 $TMP/hdr 与 $TMP/resp
  local p=$1 j=$2 path=$3; shift 3
  local t; t=$(token "$p" "$j")
  curl -s -b "$j" -c "$j" -D "$TMP/hdr" -o "$TMP/resp" -w "%{http_code}" --max-time 15 \
    "$@" --data-urlencode "_csrf=$t" "http://localhost:$p$path"
}
loc() { grep -i '^Location:' "$TMP/hdr" | tr -d '\r' | sed -E 's/^Location: //; s#^https?://[^/]+##'; }
flash() { # port jar -> 跟随 Location 抓一次性 flash 提示
  local p=$1 j=$2 path; path=$(loc)
  [ -z "$path" ] && path=/my/home
  curl -s -b "$j" -c "$j" -o "$TMP/flash" --max-time 10 "http://localhost:$p$path"
  grep -oE 'alert alert-(success|error)[^>]*>[^<]{0,90}' "$TMP/flash" | head -1 | sed -E 's/.*>//'
}

echo "==================== 0. 基线 ===================="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';
     DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE u.username IN ('admin02','probe_emp');
     DELETE FROM sys_user WHERE username IN ('admin02','probe_emp');
     INSERT INTO schedule(employee_id,work_date,shift_id,is_rest,source,remark)
       VALUES(21,'$D',1,0,'MANUAL','AUDIT_FIX');
     INSERT INTO sys_user(username,password,plain_password,real_name,role,enabled,created_by,role_level)
       SELECT 'admin02', password, '123456', '回归验证管理员', 'ADMIN', 1, 'admin', 'ADMIN'
       FROM sys_user WHERE username='zhangsan' LIMIT 1;
     SET @aid = LAST_INSERT_ID();
     INSERT INTO sys_user_role(user_id,role_id) VALUES(@aid,1);" | head -2
echo "BASE_ACCOUNTS=$(sql "SELECT COUNT(*) FROM sys_user;" | tail -1)  SCHEDULE=$(sql "SELECT COUNT(*) FROM schedule WHERE work_date='$D';" | tail -1)"

echo "==================== 1. 登录三端 ===================="
echo "  SUPER : $(login 8091 superadmin "$SUPER_PASS" "$TMP/super.ck")"
echo "  ADMIN : $(login 8092 admin02 123456 "$TMP/adm.ck")"
echo "  EMP   : $(login 8093 lisi1 123456 "$TMP/emp.ck")"

echo "==================== 2. BUG-01 修复：伪造 XFF 不能再绕过围栏 ===================="
sql "DELETE FROM punch_location;
     INSERT INTO punch_location(name,address,longitude,latitude,radius_m,allow_ip,wifi_ssid,
       is_default,enabled,remark,created_by,created_by_role,updated_by,updated_by_role,update_time)
       VALUES('IP门禁点','北京',116.397128,39.916527,100,'10.99.99.99',NULL,1,1,'XFF修复验证',
              'admin','ADMIN','admin','ADMIN',NOW());" | head -1
c=$(post 8093 "$TMP/emp.ck" /my/check-in); echo "  [无 XFF]   HTTP=$c LOC=$(loc) DB=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1) 提示=$(flash 8093 "$TMP/emp.ck")"
c=$(post 8093 "$TMP/emp.ck" /my/check-in -H 'X-Forwarded-For: 10.99.99.99'); echo "  [伪造XFF]  HTTP=$c LOC=$(loc) DB=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1) 提示=$(flash 8093 "$TMP/emp.ck")"
echo "  期望：两行 HTTP=302、DB=0、提示为围栏拒绝"

echo "==================== 3. 不回归：正确 GPS 坐标仍可打卡 ===================="
sql "DELETE FROM punch_location; DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     INSERT INTO punch_location(name,address,longitude,latitude,radius_m,allow_ip,wifi_ssid,
       is_default,enabled,remark,created_by,created_by_role,updated_by,updated_by_role,update_time)
       VALUES('上海GPS点','上海',121.473701,31.230416,200,NULL,NULL,1,1,'GPS回归',
              'admin','ADMIN','admin','ADMIN',NOW());" | head -1
c=$(post 8093 "$TMP/emp.ck" /my/check-in --data-urlencode 'lat=39.916527' --data-urlencode 'lng=116.397128'); echo "  [错坐标] HTTP=$c DB=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1) 提示=$(flash 8093 "$TMP/emp.ck")"
c=$(post 8093 "$TMP/emp.ck" /my/check-in --data-urlencode 'lat=31.230416' --data-urlencode 'lng=121.473701'); echo "  [对坐标] HTTP=$c DB=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1) 提示=$(flash 8093 "$TMP/emp.ck")"
echo "  期望：错坐标 DB=0，对坐标 DB=1 且提示签到成功"

echo "==================== 4. BUG-02 修复：超管接管管理员地点后覆盖生效 ===================="
sql "DELETE FROM punch_location; DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     INSERT INTO punch_location(name,address,longitude,latitude,radius_m,allow_ip,wifi_ssid,
       is_default,enabled,remark,created_by,created_by_role,updated_by,updated_by_role,update_time)
       VALUES('管理员原点','上海',121.473701,31.230416,200,NULL,NULL,1,1,'待超管接管',
              'admin','ADMIN','admin','ADMIN',NOW());" | head -1
AID=$(sql "SELECT id FROM punch_location WHERE name='管理员原点';" | tail -1)
echo "  地点ID=$AID  接管前 created_by_role=$(sql "SELECT created_by_role FROM punch_location WHERE id=$AID;" | tail -1)"
c=$(post 8091 "$TMP/super.ck" /super/locations/default/$AID); echo "  超管设为默认 HTTP=$c LOC=$(loc)"
echo "  接管后 created_by_role=$(sql "SELECT created_by_role FROM punch_location WHERE id=$AID;" | tail -1)"
echo "  findSuperDefault 命中数=$(sql "SELECT COUNT(*) FROM punch_location WHERE enabled=1 AND is_default=1 AND created_by_role='SUPER_ADMIN';" | tail -1)（期望 1）"
c=$(post 8092 "$TMP/adm.ck" /admin/locations/toggle/$AID --data-urlencode 'enabled=0'); echo "  管理员尝试停用 HTTP=$c 提示=$(flash 8092 "$TMP/adm.ck")"
echo "  该地点 enabled 仍为 $(sql "SELECT enabled FROM punch_location WHERE id=$AID;" | tail -1)（期望 1，层级保护生效）"

echo "==================== 5. BUG-03 修复：角色错配账号不能跨端越权 ===================="
sql "INSERT INTO sys_user(username,password,plain_password,real_name,role,enabled,created_by,role_level)
       SELECT 'probe_emp', password, '123456', '错配探针', 'EMPLOYEE', 1, 'admin', 'EMPLOYEE'
       FROM sys_user WHERE username='lisi1' LIMIT 1;
     SET @pid = LAST_INSERT_ID();
     INSERT INTO sys_user_role(user_id,role_id) VALUES(@pid,5);" | head -1
echo "  错配账号登录 8093: $(login 8093 probe_emp 123456 "$TMP/pe.ck")"
for u in /super/admins /super/employees /admin/system/users; do
  echo "    $u = $(curl -s -b "$TMP/pe.ck" -o /dev/null -w '%{http_code}' --max-time 10 "http://localhost:8093$u")（期望 403）"
done

echo "==================== 6. BUG-04/05/11：参数校验、500 状态码、业务异常回源 ===================="
c=$(post 8093 "$TMP/emp.ck" /my/trip/apply -H "Referer: http://localhost:8093/my/home" --data-urlencode 'destination=X' --data-urlencode "startDate=$D" --data-urlencode 'endDate=2000-01-01' --data-urlencode 'reason=x'); echo "  [有Referer] 出差日期倒挂 HTTP=$c LOC=$(loc) 提示=$(flash 8093 "$TMP/emp.ck")"
c=$(post 8093 "$TMP/emp.ck" /my/trip/apply --data-urlencode 'destination=X' --data-urlencode "startDate=$D" --data-urlencode 'endDate=2000-01-01' --data-urlencode 'reason=x'); echo "  [无Referer] 出差日期倒挂 HTTP=$c LOC=$(loc) 提示=$(flash 8093 "$TMP/emp.ck")（期望 LOC=/my/home，不再是 /dashboard）"
c=$(post 8093 "$TMP/emp.ck" /my/overtime/apply --data-urlencode "otDate=$D" --data-urlencode 'hours=-5' --data-urlencode 'otType=X' --data-urlencode 'compensateType=PAY' --data-urlencode 'reason=x'); echo "  [加班负时长] HTTP=$c LOC=$(loc) 提示=$(flash 8093 "$TMP/emp.ck")"
c=$(post 8093 "$TMP/emp.ck" /my/appeal --data-urlencode 'recordId=abc' --data-urlencode 'reason=x'); echo "  [非法Long参数] HTTP=$c（期望 500，不是 200） 页面=$(grep -oE '系统处理请求时出错|参数格式' "$TMP/resp" | head -1)"

echo "==================== 7. 权限隔离不回归 ===================="
echo "  员工 GET /super/admins       = $(curl -s -b "$TMP/emp.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8093/super/admins)"
echo "  员工 GET /admin/system/users = $(curl -s -b "$TMP/emp.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8093/admin/system/users)"
echo "  管理员 GET /super/admins     = $(curl -s -b "$TMP/adm.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8092/super/admins)"
echo "  超管 GET /super/dashboard    = $(curl -s -b "$TMP/super.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8091/super/dashboard)"
echo "  超管 GET /my/home            = $(curl -s -b "$TMP/super.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8091/my/home)"
echo "  员工 GET /my/home            = $(curl -s -b "$TMP/emp.ck" -o /dev/null -w '%{http_code}' --max-time 10 http://localhost:8093/my/home)"

echo "==================== 8. 清理 ===================="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';
     DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE u.username IN ('admin02','probe_emp');
     DELETE FROM sys_user WHERE username IN ('admin02','probe_emp');" | head -1
echo "  CLEANED  accounts=$(sql "SELECT COUNT(*) FROM sys_user;" | tail -1)  locations=$(sql "SELECT COUNT(*) FROM punch_location;" | tail -1)"
