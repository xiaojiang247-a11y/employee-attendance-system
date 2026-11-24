package com.example.attendance.config;

import com.example.attendance.entity.User;
import com.example.attendance.mapper.UserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 组装角色与菜单权限，供 URL 拦截和页面 sec:authorize 使用 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("用户不存在或已停用: " + username);

        List<GrantedAuthority> authorities = new ArrayList<>();
        // 主角色只取 sys_user.role 一个来源：PortRoleGuard 按「第一个 ROLE_」判定端口，
        // SecurityConfig 按全部 authorities 判定 URL。若再并入 sys_user_role 的 ROLE_，
        // 两处角色集合不同源，错配数据即可在员工端口获得超管权限。
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        for (String perm : userMapper.findPermCodes(user.getId())) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        boolean enabled = !Boolean.FALSE.equals(user.getEnabled());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), enabled,
                true, true, true, authorities);
    }
}
