#!/usr/bin/env bash
# 三端口 V4 围栏闭环验证：无地点拒绝 / 管理员设点 / 错对坐标 / 超管默认覆盖 / 层级保护
# 用法：bash fence_check.sh   依赖 curl + mysql，时间口径 Asia/Shanghai
# 前提：交付库已清空全部管理员/员工账号，运行前需先在超管后台自行创建
#       管理员(admin02)与员工(lisi1)测试账号；脚本不再预置任何账号。
set -u
: "${DB_USERNAME:=root}"
: "${DB_PASSWORD:=}"
: "${SUPER_PASS:=ChangeMe@2026}"
: "${MYSQL_BIN:=mysql}"
: "${TMPDIR:=/tmp}"
MYSQL="$MYSQL_BIN"
SUPER=8081; ADMIN=8082; EMP=8083
TMP=""${TMPDIR}/attendance-test""
mkdir -p "$TMP"
JAR_SUP="$TMP/super.cookies"; JAR_ADM="$TMP/adm.cookies"; JAR_EMP="$TMP/emp.cookies"
rm -f "$JAR_SUP" "$JAR_ADM" "$JAR_EMP"

sql() { "$MYSQL" -u"$DB_USERNAME" -p"$DB_PASSWORD" --default-character-set=utf8mb4 -e "USE attendance_db; $1" 2>&1 | grep -v "Warning" ; }

# 从页面里取 CSRF（Spring Security 6 渲染的隐藏域）
csrf() { grep -o 'name="_csrf"[^>]*value="[^"]*"' "$1" | head -1 | sed 's/.*value="\([^"]*\)".*/\1/'; }

# login <port> <user> <pass> <jar> -> 打印最终 URL
login() {
  local port=$1 user=$2 pass=$3 jar=$4 page="$TMP/lg_${1}.html" hdr="$TMP/lg_${1}.hdr"
  curl -s -c "$jar" "http://localhost:${port}/login" -o "$page"
  local t; t=$(csrf "$page")
  curl -s -b "$jar" -c "$jar" -D "$hdr" -o /dev/null \
    --data-urlencode "username=$user" --data-urlencode "password=$pass" --data-urlencode "_csrf=$t" \
    "http://localhost:${port}/login"
  grep -i '^Location:' "$hdr" | tr -d '\r' | sed 's/^/  /'
}

# post_form <port> <path> <jar> <data...> -> 跟随重定向后抓 flash 提示
post_form() {
  local port=$1 path=$2 jar=$3; shift 3
  local page="$TMP/p_${port}.html" hdr="$TMP/p_${port}.hdr" t
  curl -s -b "$jar" -c "$jar" "http://localhost:${port}${path%%\?*}" -o "$page" 2>/dev/null
  t=$(csrf "$page")
  local args=(); for d in "$@"; do args+=(--data-urlencode "$d"); done
  curl -s -L -b "$jar" -c "$jar" -D "$hdr" -o "$TMP/resp.html" \
    "${args[@]}" --data-urlencode "_csrf=$t" "http://localhost:${port}${path}"
  # 提取页面中的 alert 文本
  grep -oE 'class="alert alert-(success|error)"[^>]*>[^<]*' "$TMP/resp.html" \
    | sed -E 's/.*>//' | sed 's/&#[^;]*;/ /g' | head -3
}

echo "=============== 0. 基线 ==============="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='2026-11-21';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='2026-11-21';
     DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username='admin02');
     DELETE FROM sys_user WHERE username='admin02';
     INSERT INTO sys_user(username,password,plain_password,real_name,role,enabled,created_by,role_level)
       SELECT 'admin02', password, '123456', '验证用管理员', 'ADMIN', 1, 'admin', 'ADMIN'
       FROM sys_user WHERE username='zhangsan' LIMIT 1;
     INSERT INTO sys_user_role(user_id, role_id)
       SELECT u.id, 1 FROM sys_user u WHERE u.username='admin02';
     INSERT INTO schedule(employee_id,work_date,shift_id,is_rest,source,remark)
       VALUES(21,'2026-11-21',1,0,'MANUAL','V4围栏闭环验证');"
LOC_N=$(sql 'SELECT COUNT(*) FROM punch_location' | tail -1)
SCH_N=$(sql "SELECT COUNT(*) FROM schedule WHERE employee_id=21 AND work_date='2026-11-21'" | tail -1)
echo "  地点数=$LOC_N  今日排班=$SCH_N"

echo "=============== 1. 登录三端口 ==============="
echo "[超管 8081]"; login $SUPER superadmin "$SUPER_PASS" "$JAR_SUP"
echo "[管理员 8082]"; login $ADMIN admin02 123456 "$JAR_ADM"
echo "[员工 8083]"; login $EMP lisi1 123456 "$JAR_EMP"

echo "=============== 2. 无地点打卡 → 应拒绝 ==============="
post_form $EMP "/my/check-in" "$JAR_EMP" "lat=31.230416" "lng=121.473701"

echo "=============== 3. 管理员设置地点 A（上海，半径 200m） ==============="
post_form $ADMIN "/admin/locations/save" "$JAR_ADM" \
  "name=管理员地点A" "address=上海市黄浦区" "latitude=31.230416" "longitude=121.473701" \
  "radiusM=200" "allowIp=" "wifiSsid=" "isDefault=1" "enabled=1" "remark=V4验证"
sql "SELECT id,name,latitude,longitude,radius_m,is_default,enabled,created_by,created_by_role FROM punch_location;" | tail -5

echo "=============== 4a. 错误坐标（北京）→ 应拒绝 ==============="
post_form $EMP "/my/check-in" "$JAR_EMP" "lat=39.904200" "lng=116.407400"

echo "=============== 4b. 正确坐标（上海）→ 应成功 ==============="
post_form $EMP "/my/check-in" "$JAR_EMP" "lat=31.230416" "lng=121.473701"
sql "SELECT id,employee_id,attendance_date,check_in_time,location_id,punch_lat,punch_lng,distance_m,status FROM attendance_record WHERE employee_id=21 AND attendance_date='2026-11-21';" | tail -3

echo "=============== 5. 超管设置地点 B（北京）并设为默认 = 覆盖管理员 ==============="
sql "DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='2026-11-21';"
post_form $SUPER "/super/locations/save" "$JAR_SUP" \
  "name=超管地点B" "address=北京市东城区" "latitude=39.904200" "longitude=116.407400" \
  "radiusM=200" "allowIp=" "wifiSsid=" "isDefault=1" "enabled=1" "remark=V4覆盖验证"
sql "SELECT id,name,latitude,longitude,is_default,created_by,created_by_role,update_time FROM punch_location ORDER BY id;" | tail -5

echo "=============== 6a. 覆盖后：管理员地点A 坐标 → 应拒绝 ==============="
post_form $EMP "/my/check-in" "$JAR_EMP" "lat=31.230416" "lng=121.473701"

echo "=============== 6b. 覆盖后：超管地点B 坐标 → 应成功 ==============="
post_form $EMP "/my/check-in" "$JAR_EMP" "lat=39.904200" "lng=116.407400"
sql "SELECT location_id,punch_lat,punch_lng,distance_m FROM attendance_record WHERE employee_id=21 AND attendance_date='2026-11-21';" | tail -2

echo "=============== 7. 管理员尝试改超管创建的地点 → 应被拒 ==============="
BID=$(sql "SELECT id FROM punch_location WHERE name='超管地点B'" | tail -1)
post_form $ADMIN "/admin/locations/save" "$JAR_ADM" \
  "id=$BID" "name=管理员偷偷改名" "address=篡改" "latitude=39.904200" "longitude=116.407400" \
  "radiusM=9999" "allowIp=" "wifiSsid=" "isDefault=0" "enabled=1" "remark=越权测试"
sql "SELECT id,name,created_by_role,update_time FROM punch_location ORDER BY id;" | tail -5

echo "=============== 8. 权限隔离复核 ==============="
echo "[员工 GET /super/dashboard] $(curl -s -o /dev/null -w '%{http_code}' -b "$JAR_EMP" http://localhost:8083/super/dashboard)"
echo "[员工 GET /admin/system/users] $(curl -s -o /dev/null -w '%{http_code}' -b "$JAR_EMP" http://localhost:8083/admin/system/users)"
echo "[管理员 GET /super/admins] $(curl -s -o /dev/null -w '%{http_code}' -b "$JAR_ADM" http://localhost:8082/super/admins)"
echo "[超管 GET /super/dashboard] $(curl -s -o /dev/null -w '%{http_code}' -b "$JAR_SUP" http://localhost:8081/super/dashboard)"

echo "=============== 8b. POST 越权 403 页文案（accessDeniedPage 保留 POST 方法） ==============="
curl -s -b "$JAR_EMP" -o "$TMP/post403.html" -w "STATUS=%{http_code} SIZE=%{size_download}\n" \
  -X POST --data-urlencode "name=x" "http://localhost:8083/super/admins/save"
grep -oE "err-code[^<]*>[^<]*|当前角色无权访问|系统处理请求时出错|Request method" "$TMP/post403.html" | head -5

echo "=============== 9. 清理测试数据（恢复基线） ==============="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='2026-11-21';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='2026-11-21';
     DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username='admin02');
     DELETE FROM sys_user WHERE username='admin02';"
echo "  地点数=$(sql 'SELECT COUNT(*) FROM punch_location' | tail -1)  账号=$(sql 'SELECT GROUP_CONCAT(username) FROM sys_user' | tail -1)"
echo "DONE"
