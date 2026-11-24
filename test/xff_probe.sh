#!/usr/bin/env bash
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
echo "DATE=$D"

echo "=== 0. 基线：今日排班 + 仅 IP 白名单的地点（10.99.99.99），无可用 GPS ==="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';
     INSERT INTO schedule(employee_id,work_date,shift_id,is_rest,source,remark)
       VALUES(21,'$D',1,0,'MANUAL','AUDIT_XFF');
     INSERT INTO punch_location(name,address,longitude,latitude,radius_m,allow_ip,wifi_ssid,
       is_default,enabled,remark,created_by,created_by_role,updated_by,updated_by_role,update_time)
       VALUES('审计IP点','北京',116.397128,39.916527,100,'10.99.99.99',NULL,1,1,'XFF审计',
              'admin','ADMIN','admin','ADMIN',NOW());" | head -2

echo "=== 1. 登录 8083（lisi1） ==="
rm -f x.ck x.lg x.hdr
curl -s -c x.ck --max-time 8 "http://localhost:8083/login" -o x.lg
T=$(csrf x.lg); echo "LOGIN_CSRF_LEN=${#T}"
curl -s -b x.ck -c x.ck -D x.hdr -o /dev/null --max-time 10 \
  --data-urlencode "username=lisi1" --data-urlencode "password=123456" --data-urlencode "_csrf=$T" \
  "http://localhost:8083/login"
echo "LOGIN_LOCATION=$(grep -i '^Location:' x.hdr | tr -d '\r')"

echo "=== 2. 登录后取当前会话 CSRF（login 页模板已缓存，200） ==="
curl -s -b x.ck -c x.ck --max-time 8 "http://localhost:8083/login" -o x.lg2 -w "LOGIN2_STATUS=%{http_code}\n"
T2=$(csrf x.lg2); echo "SESSION_CSRF_LEN=${#T2}"

echo "=== 3. 对照组：无 XFF、无坐标（应拒绝，DB 0 条） ==="
curl -s -b x.ck -c x.ck -o /dev/null -w "NO_XFF_HTTP=%{http_code}\n" --max-time 10 \
  -X POST --data-urlencode "_csrf=$T2" "http://localhost:8083/my/check-in"
echo "NO_XFF_DB_COUNT=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1)"

echo "=== 4. 实验组：仅加 X-Forwarded-For 头（若落库=围栏被绕过） ==="
curl -s -b x.ck -c x.ck -o /dev/null -w "XFF_HTTP=%{http_code}\n" --max-time 10 \
  -X POST -H "X-Forwarded-For: 10.99.99.99" --data-urlencode "_csrf=$T2" \
  "http://localhost:8083/my/check-in"
echo "--- DB after XFF ---"
sql "SELECT id,employee_id,attendance_date,check_in_time,check_in_ip,status,punch_address FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';"

echo "=== 5. 清理 ==="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';" | head -1
echo "CLEANED"
