package com.example.attendance.service;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 系统管理：菜单树、角色授权、账号维护 */
@Service
public class SystemService {

    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final EmployeeMapper employeeMapper;
    private final SecurityHelper security;

    public SystemService(UserMapper userMapper, MenuMapper menuMapper,
                         RoleMapper roleMapper, EmployeeMapper employeeMapper,
                         SecurityHelper security) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.employeeMapper = employeeMapper;
        this.security = security;
    }

    /** 把平铺菜单组装成两级树，侧边栏直接遍历 DIR -> children */
    public List<Menu> menuTree(List<Menu> flat) {
        Map<Long, Menu> dirs = new LinkedHashMap<>();
        List<Menu> children = new ArrayList<>();
        for (Menu m : flat) {
            if ("DIR".equals(m.getMenuType())) {
                m.setChildren(new ArrayList<>());
                dirs.put(m.getId(), m);
            } else children.add(m);
        }
        for (Menu c : children) {
            Menu parent = dirs.get(c.getParentId() == null ? 0L : c.getParentId());
            if (parent != null) parent.getChildren().add(c);
        }
        return new ArrayList<>(dirs.values());
    }

    public List<Menu> currentUserMenuTree() {
        User u = security.currentUser();
        if (u == null) return Collections.emptyList();
        return menuTree(userMapper.findMenusByUserId(u.getId()));
    }

    @Transactional
    public void grantMenus(Long roleId, List<Long> menuIds) {
        roleMapper.clearMenus(roleId);
        if (menuIds == null) return;
        for (Long id : menuIds) roleMapper.grantMenu(roleId, id);
    }

    @Transactional
    public void saveUser(User u, Long roleId, PasswordEncoder encoder, boolean create) {
        if (create) {
            if (userMapper.findByUsernameIgnoreEnabled(u.getUsername()) != null) {
                throw new BizException("登录账号 " + u.getUsername() + " 已存在");
            }
            u.setPassword(encoder.encode(u.getPlainPassword() == null ? "123456" : u.getPlainPassword()));
            userMapper.insert(u);
        } else {
            userMapper.update(u);
        }
        Role role = roleMapper.findById(roleId);
        if (role != null) {
            userMapper.deleteRoles(u.getId());
            u.setRole(role.getCode());
            userMapper.update(u);
            userMapper.grantRole(u.getId(), roleId);
        }
    }

    public List<User> users(String keyword, Long deptId) {
        return userMapper.findAll(keyword, deptId);
    }
}
