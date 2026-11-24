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
D=$(date +%F); T=""${TMPDIR}/attendance-test"/pre"; rm -rf "$T"; mkdir -p "$T"

echo "=== 基线：今日排班 + 仅 IP 白名单地点(10.99.99.99) ==="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';
     INSERT INTO schedule(employee_id,work_date,shift_id,is_rest,source,remark)
       VALUES(21,'$D',1,0,'MANUAL','PRECISE');
     INSERT INTO punch_location(name,address,longitude,latitude,radius_m,allow_ip,wifi_ssid,
       is_default,enabled,remark,created_by,created_by_role,updated_by,updated_by_role,update_time)
       VALUES('IP门禁点','北京',116.397128,39.916527,100,'10.99.99.99',NULL,1,1,'精确探针','admin','ADMIN','admin','ADMIN',NOW());" | head -1

echo "=== 登录 8093 ==="
curl -s -c "$T/ck" --max-time 10 "http://localhost:8093/login" -o "$T/lg"
t=$(csrf "$T/lg")
curl -s -b "$T/ck" -c "$T/ck" -D "$T/hdr" -o /dev/null --max-time 10 \
  --data-urlencode "username=lisi1" --data-urlencode "password=123456" --data-urlencode "_csrf=$t" \
  "http://localhost:8093/login"
echo "LOGIN=$(grep -i '^Location:' "$T/hdr" | tr -d '\r')"

echo "=== GET /my/home（真实表单页取 CSRF） ==="
curl -s -b "$T/ck" -c "$T/ck" -o "$T/home" -w "HOME_STATUS=%{http_code} SIZE=%{size_download}\n" --max-time 10 \
  "http://localhost:8093/my/home"
t2=$(csrf "$T/home"); echo "HOME_CSRF_LEN=${#t2}  CSRF_FORMS=$(grep -c 'name=\"_csrf\"' "$T/home")"

punch() { # $1 标签  $2.. 额外参数
  local tag=$1; shift
  curl -s -b "$T/ck" -c "$T/ck" -D "$T/p.hdr" -o /dev/null --max-time 10 \
    "$@" --data-urlencode "_csrf=$t2" "http://localhost:8093/my/check-in"
  local loc; loc=$(grep -i '^Location:' "$T/p.hdr" | tr -d '\r' | sed 's/^Location: //')
  curl -s -b "$T/ck" -c "$T/ck" -o "$T/resp" --max-time 10 "http://localhost:8093/my/home"
  echo "[$tag] POST_LOCATION=$loc"
  echo "[$tag] FLASH=$(grep -oE 'alert alert-(success|error)[^>]*>[^<]{0,90}' "$T/resp" | head -1 | sed -E 's/.*>//')"
  echo "[$tag] DB_COUNT=$(sql "SELECT COUNT(*) FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';" | tail -1)"
}

echo "=== BUG-01 精确验证 ==="
punch "无XFF"
punch "伪造XFF=10.99.99.99" -H "X-Forwarded-For: 10.99.99.99"

echo "=== BUG-04 精确验证（带 Referer，模拟真实浏览器） ==="
apply() { # $1 标签 $2 路径 $3.. 数据
  local tag=$1 path=$2; shift 2
  # 取来源页 CSRF
  curl -s -b "$T/ck" -c "$T/ck" -o "$T/src" --max-time 10 "http://localhost:8093${path%/apply*}"
  local tk; tk=$(csrf "$T/src")
  [ -z "$tk" ] && tk=$t2
  curl -s -b "$T/ck" -c "$T/ck" -D "$T/a.hdr" -o /dev/null --max-time 10 \
    -H "Referer: http://localhost:8093/my/home" \
    "$@" --data-urlencode "_csrf=$tk" "http://localhost:8093$path"
  local loc; loc=$(grep -i '^Location:' "$T/a.hdr" | tr -d '\r' | sed 's/^Location: //')
  # 跟随到目标页抓 flash
  curl -s -b "$T/ck" -c "$T/ck" -o "$T/ar" -w "" --max-time 10 "http://localhost:8093${loc#http://localhost:8093}" 2>/dev/null
  echo "[$tag] LOCATION=$loc"
  echo "[$tag] FLASH=$(grep -oE 'alert alert-(success|error)[^>]*>[^<]{0,90}' "$T/ar" | head -1 | sed -E 's/.*>//')"
}
apply "出差日期倒挂" "/my/trip/apply" --data-urlencode "destination=X" --data-urlencode "startDate=$D" --data-urlencode "endDate=2000-01-01" --data-urlencode "reason=x"
apply "加班负时长" "/my/overtime/apply" --data-urlencode "otDate=$D" --data-urlencode "hours=-5" --data-urlencode "otType=X" --data-urlencode "compensateType=PAY" --data-urlencode "reason=x"

echo "=== 清理 ==="
sql "DELETE FROM attendance_group_location; DELETE FROM punch_location;
     DELETE FROM attendance_record WHERE employee_id=21 AND attendance_date='$D';
     DELETE FROM schedule WHERE employee_id=21 AND work_date='$D';" | head -1
echo CLEANED
