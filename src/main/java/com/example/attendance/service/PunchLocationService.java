package com.example.attendance.service;

import com.example.attendance.entity.Employee;
import com.example.attendance.entity.PunchLocation;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.EmployeeMapper;
import com.example.attendance.mapper.PunchLocationMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 打卡地点围栏：按员工所属考勤组解析允许地点，用 GPS 距离 / IP 白名单 / WiFi SSID 三种因子校验。
 * 策略：未配置任何启用地点时一律拒绝（必须先由管理员在后台设定允许地点）；
 * 配置后命中任一地点任一因子即放行，全部落空则拒绝打卡。
 * 三种因子是 OR 关系，因为办公网络、GPS 漂移、终端拿不到定位是常见现实情况。
 */
@Service
public class PunchLocationService {

    private static final double EARTH_R = 6371000.0;

    private final PunchLocationMapper locationMapper;
    private final EmployeeMapper employeeMapper;

    public PunchLocationService(PunchLocationMapper locationMapper, EmployeeMapper employeeMapper) {
        this.locationMapper = locationMapper;
        this.employeeMapper = employeeMapper;
    }

    /** 校验结果：通过时带地点与距离，写入考勤流水留痕 */
    public static class Result {
        private Long locationId;
        private String locationName;
        private Integer distanceM;
        private String matchType;

        public Result() {}
        public Result(Long id, String name, Integer dist, String type) {
            this.locationId = id;
            this.locationName = name;
            this.distanceM = dist;
            this.matchType = type;
        }
        public Long getLocationId() { return locationId; }
        public String getLocationName() { return locationName; }
        public Integer getDistanceM() { return distanceM; }
        public String getMatchType() { return matchType; }
        public void setLocationId(Long v) { this.locationId = v; }
        public void setLocationName(String v) { this.locationName = v; }
        public void setDistanceM(Integer v) { this.distanceM = v; }
        public void setMatchType(String v) { this.matchType = v; }
    }

    public Result verify(Long employeeId, Double lat, Double lng, String ip, String wifiSsid) {
        List<PunchLocation> candidates = candidatesFor(employeeId);
        // 用户要求「必须在指定地点才能打卡」：没配地点就不能放行，避免围栏静默失效
        if (candidates.isEmpty()) {
            throw new BizException("打卡失败：管理员尚未配置允许打卡的地点，请先在「打卡地点」中设置后再打卡");
        }

        int nearest = Integer.MAX_VALUE;
        String nearestName = null;
        for (PunchLocation l : candidates) {
            // IP 白名单是唯一来自网络层、服务端可校验的因子：一旦配置就必须命中。
            // 否则 GPS 坐标与 WiFi SSID 都是客户端表单上报值（可任意伪造），
            // 单独命中就放行等于围栏完全失效。
            boolean hasIpRule = hasText(l.getAllowIp());
            boolean ipOk = hasIpRule && ip != null && ipMatch(l.getAllowIp(), ip);
            if (hasIpRule && !ipOk) continue;

            if (l.getLongitude() != null && l.getLatitude() != null && lat != null && lng != null
                    && validLatLng(lat, lng)) {
                int d = (int) Math.round(haversine(lat, lng,
                        l.getLatitude().doubleValue(), l.getLongitude().doubleValue()));
                if (d < nearest) { nearest = d; nearestName = l.getName(); }
                int radius = (l.getRadiusM() == null || l.getRadiusM() <= 0) ? 200 : l.getRadiusM();
                if (d <= radius) return new Result(l.getId(), l.getName(), d, "GPS");
            }
            if (ipOk) return new Result(l.getId(), l.getName(), null, "IP");
            // WiFi SSID 由浏览器表单上报（浏览器本身无法读取 SSID），不再作为放行依据；
            // 历史流水的 WIFI 匹配类型仍由 matchLabel 兼容展示。
            // 只填名称、未配置可信因子的地点属于无效配置，跳过而不是放行
        }
        String hint = nearest == Integer.MAX_VALUE
                ? "未获取到定位信息，请允许浏览器定位或连接办公网络"
                : "距「" + nearestName + "」约 " + nearest + " 米";
        throw new BizException("打卡失败：当前位置不在允许的打卡范围内（" + hint + "），请到指定地点后重试");
    }

    /** 候选地点：超管默认地点（覆盖）> 考勤组绑定 > 默认地点 > 全部启用地点兜底 */
    private List<PunchLocation> candidatesFor(Long employeeId) {
        // 超级管理员把某地点设为默认后，只有该地点生效：这就是「超管覆盖管理员设置」的落点，
        // 管理员配置的其它地点不再参与校验，避免围栏被旧配置悄悄放宽。
        PunchLocation superDefault = locationMapper.findSuperDefault();
        if (superDefault != null) {
            List<PunchLocation> only = new ArrayList<>();
            only.add(superDefault);
            return only;
        }
        List<PunchLocation> list = new ArrayList<>();
        Employee e = employeeId == null ? null : employeeMapper.findById(employeeId);
        if (e != null && e.getGroupId() != null) {
            List<PunchLocation> byGroup = locationMapper.findByGroup(e.getGroupId());
            if (byGroup != null) list.addAll(byGroup);
        }
        PunchLocation def = locationMapper.findDefault();
        if (def != null && list.stream().noneMatch(x -> def.getId().equals(x.getId()))) list.add(def);
        if (list.isEmpty()) {
            List<PunchLocation> all = locationMapper.findEnabled();
            if (all != null) list.addAll(all);
        }
        return list;
    }

    /** Haversine 球面距离，返回米；坐标精度到 1e-6 时误差在米级 */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_R * Math.asin(Math.sqrt(a));
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    /** 客户端上报坐标先做数值范围校验，避免非法值写进流水或让距离计算失真 */
    private static boolean validLatLng(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /** 支持精确 IP、192.168.1.* 通配、192.168.1.0/24 CIDR，多条用逗号分隔 */
    private static boolean ipMatch(String rule, String ip) {
        for (String part : rule.split("[,，;\\s]+")) {
            String r = part.trim();
            if (r.isEmpty() || r.equals("*")) continue;
            if (r.equals(ip)) return true;
            if (r.endsWith(".*") && ip.startsWith(r.substring(0, r.length() - 1))) return true;
            if (r.contains("/")) {
                String[] seg = r.split("/");
                try {
                    if (seg.length == 2 && maskMatch(seg[0], ip, Integer.parseInt(seg[1]))) return true;
                } catch (NumberFormatException ignored) { }
            }
        }
        return false;
    }

    private static boolean maskMatch(String net, String ip, int bits) {
        if (bits < 0 || bits > 32) return false;
        try {
            long a = ipToLong(net), b = ipToLong(ip);
            long mask = bits == 0 ? 0L : ((0xFFFFFFFFL << (32 - bits)) & 0xFFFFFFFFL);
            return (a & mask) == (b & mask);
        } catch (Exception e) { return false; }
    }

    private static long ipToLong(String s) {
        String[] parts = s.trim().split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("not ipv4: " + s);
        long v = 0;
        for (String p : parts) v = (v << 8) | (Long.parseLong(p.trim()) & 0xFF);
        return v & 0xFFFFFFFFL;
    }

    public static String matchLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "GPS": return "GPS定位";
            case "IP": return "IP白名单";
            case "WIFI": return "办公WiFi";
            case "OPEN": return "登记点";
            default: return "";
        }
    }
}
