package com.nz.admin.config;

import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.modules.system.entity.dataobject.config.ConfigDO;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import com.nz.admin.modules.system.entity.dataobject.dict.DictDataDO;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.notice.NoticeDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;
import com.nz.admin.modules.system.service.config.ConfigService;
import com.nz.admin.modules.system.service.dept.DeptService;
import com.nz.admin.modules.system.service.dept.PostService;
import com.nz.admin.modules.system.service.dict.DictDataService;
import com.nz.admin.modules.system.service.dict.DictTypeService;
import com.nz.admin.modules.system.service.menu.MenuService;
import com.nz.admin.modules.system.service.notice.NoticeService;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.role.RoleService;
import com.nz.admin.modules.system.service.job.JobService;
import com.nz.admin.modules.system.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private DeptService deptService;
    @Autowired
    private PostService postService;
    @Autowired
    private DictTypeService dictTypeService;
    @Autowired
    private DictDataService dictDataService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private JobService jobService;
    @Autowired
    private PermissionService permissionService;

    @Override
    public void run(String... args) {
        log.info("初始化基础数据...");

        DeptDO rootDept = ensureRootDept();
        ensurePosts();
        UserDO admin = ensureAdminUser(rootDept);
        RoleDO adminRole = ensureAdminRole();
        ensureMenus();
        ensureDicts();
        ensureConfigs();
        ensureNotice();
        ensureJob();

        // 分配管理员所有菜单权限
        var allMenus = menuService.listAll();
        roleService.assignMenus(adminRole.getId(), allMenus.stream().map(MenuDO::getId).toList());

        // 分配管理员角色
        permissionService.assignUserRoles(admin.getId(), java.util.List.of(adminRole.getId()));

        // Initialize Quartz scheduler with all active jobs
        var allJobs = jobService.listPage(1, 200, null, null, null).getRecords();
        try {
            jobService.initializeScheduler(allJobs);
        } catch (Exception e) {
            log.error("初始化 Quartz 调度失败", e);
        }

        log.info("初始化完成: admin / admin123");
    }

    private DeptDO ensureRootDept() {
        return deptService.listAll().stream()
                .filter(dept -> Objects.equals(0L, dept.getParentId()) && "总公司".equals(dept.getName()))
                .findFirst()
                .orElseGet(() -> {
                    DeptDO rootDept = new DeptDO();
                    rootDept.setParentId(0L);
                    rootDept.setName("总公司");
                    rootDept.setSort(0);
                    rootDept.setStatus(0);
                    deptService.save(rootDept);
                    return rootDept;
                });
    }

    private UserDO ensureAdminUser(DeptDO rootDept) {
        UserDO admin = userService.getByUsername("admin");
        if (admin != null) {
            return admin;
        }
        admin = new UserDO();
        admin.setDeptId(rootDept.getId());
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setNickname("管理员");
        admin.setStatus(0);
        userService.save(admin);
        return admin;
    }

    private RoleDO ensureAdminRole() {
        return roleService.listAll().stream()
                .filter(role -> "admin".equals(role.getRoleKey()))
                .findFirst()
                .orElseGet(() -> {
                    RoleDO adminRole = new RoleDO();
                    adminRole.setName("超级管理员");
                    adminRole.setRoleKey("admin");
                    adminRole.setSort(0);
                    adminRole.setStatus(0);
                    adminRole.setRemark("系统内置角色，拥有全部权限");
                    roleService.save(adminRole);
                    return adminRole;
                });
    }

    private void ensureMenus() {
        MenuDO systemDir = ensureMenu(0L, "系统管理", "/system", null, "Setting", 0, "M", null, 0);
        ensureCrudMenu(systemDir.getId(), "用户管理", "user", "system/user/index", "User", 1, "system:user", true);
        ensureCrudMenu(systemDir.getId(), "角色管理", "role", "system/role/index", "UserFilled", 2, "system:role", true);
        ensureCrudMenu(systemDir.getId(), "菜单管理", "menu", "system/menu/index", "Menu", 3, "system:menu", true);
        ensureCrudMenu(systemDir.getId(), "部门管理", "dept", "system/dept/index", "OfficeBuilding", 4, "system:dept", true);
        ensureCrudMenu(systemDir.getId(), "岗位管理", "post", "system/post/index", "User", 5, "system:post", true);
        ensureCrudMenu(systemDir.getId(), "字典管理", "dict", "system/dict/index", "Collection", 6, "system:dict", true);
        ensureCrudMenu(systemDir.getId(), "参数管理", "config", "system/config/index", "Tools", 7, "system:config", true);
        ensureCrudMenu(systemDir.getId(), "通知公告", "notice", "system/notice/index", "Bell", 8, "system:notice", true);
        ensureCrudMenu(systemDir.getId(), "文件管理", "file", "system/file/index", "Folder", 9, "system:file", false);

        MenuDO monitorDir = ensureMenu(0L, "系统监控", "/monitor", null, "Monitor", 20, "M", null, 1);
        ensureCrudMenu(monitorDir.getId(), "操作日志", "oper-log", null, "Document", 1, "system:operlog", false);
        ensureCrudMenu(monitorDir.getId(), "登录日志", "login-log", null, "Tickets", 2, "system:loginlog", false);
        ensureCrudMenu(monitorDir.getId(), "在线用户", "online", null, "Connection", 3, "system:online", false);
        ensureCrudMenu(monitorDir.getId(), "定时任务", "job", null, "Timer", 4, "system:job", true);
    }

    private void ensureCrudMenu(Long parentId, String name, String path, String component, String icon, int sort, String permPrefix, boolean withMutationButtons) {
        int visible = component == null ? 1 : 0;
        MenuDO menu = ensureMenu(parentId, name, path, component, icon, sort, "C", permPrefix + ":list", visible);
        saveButtons(menu.getId(), permPrefix, sort + 1, withMutationButtons);
    }

    private MenuDO ensureMenu(Long parentId, String name, String path, String component, String icon, int sort, String type, String perm, int visible) {
        return menuService.listAll().stream()
                .filter(menu -> Objects.equals(parentId, menu.getParentId()) && name.equals(menu.getName()))
                .findFirst()
                .orElseGet(() -> {
                    MenuDO menu = createMenu(parentId, name, path, component, icon, sort, type, perm, visible);
                    menuService.save(menu);
                    return menu;
                });
    }

    private void ensurePosts() {
        if (postService.listAll().stream().noneMatch(post -> "ceo".equals(post.getPostCode()))) {
            savePost("ceo", "总经理", 1, "系统内置岗位");
        }
        if (postService.listAll().stream().noneMatch(post -> "dev".equals(post.getPostCode()))) {
            savePost("dev", "开发工程师", 2, "系统内置岗位");
        }
    }

    private void savePost(String code, String name, int sort, String remark) {
        PostDO post = new PostDO();
        post.setPostCode(code);
        post.setPostName(name);
        post.setSort(sort);
        post.setStatus(0);
        post.setRemark(remark);
        postService.save(post);
    }

    private void ensureDicts() {
        ensureDictType("系统状态", "sys_normal_disable", "系统通用状态");
        ensureDictData("sys_normal_disable", "正常", "0", 1);
        ensureDictData("sys_normal_disable", "停用", "1", 2);
        ensureDictType("是否状态", "sys_yes_no", "系统通用是否");
        ensureDictData("sys_yes_no", "是", "Y", 1);
        ensureDictData("sys_yes_no", "否", "N", 2);
        ensureDictType("通知类型", "sys_notice_type", "通知公告类型");
        ensureDictData("sys_notice_type", "通知", "1", 1);
        ensureDictData("sys_notice_type", "公告", "2", 2);
    }

    private void ensureDictType(String name, String type, String remark) {
        DictTypeQuery query = new DictTypeQuery();
        query.setPageSize(200);
        boolean exists = dictTypeService.listPage(query).getRecords().stream()
                .anyMatch(dictType -> type.equals(dictType.getType()));
        if (exists) return;
        DictTypeDO dictType = new DictTypeDO();
        dictType.setName(name);
        dictType.setType(type);
        dictType.setStatus(0);
        dictType.setRemark(remark);
        dictTypeService.save(dictType);
    }

    private void ensureDictData(String dictType, String label, String value, int sort) {
        boolean exists = dictDataService.listByDictType(dictType).stream()
                .anyMatch(dictData -> value.equals(dictData.getValue()));
        if (exists) return;
        DictDataDO dictData = new DictDataDO();
        dictData.setDictType(dictType);
        dictData.setLabel(label);
        dictData.setValue(value);
        dictData.setSort(sort);
        dictData.setStatus(0);
        dictDataService.save(dictData);
    }

    private void ensureConfigs() {
        ensureConfig("主框架页签", "sys.index.tabs", "true", "是否开启页签风格");
        ensureConfig("默认密码", "sys.user.initPassword", "123456", "新用户默认密码");
    }

    private void ensureConfig(String name, String key, String value, String remark) {
        boolean exists = configService.listPage(1, 200, null, null, null).getRecords().stream()
                .anyMatch(config -> key.equals(config.getConfigKey()));
        if (exists) return;
        ConfigDO config = new ConfigDO();
        config.setConfigName(name);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(1);
        config.setStatus(0);
        config.setRemark(remark);
        configService.save(config);
    }

    private void ensureNotice() {
        boolean exists = noticeService.listPage(new com.nz.admin.modules.system.entity.query.notice.NoticeQuery())
                .getRecords().stream()
                .anyMatch(notice -> "欢迎使用 nz-admin".equals(notice.getTitle()));
        if (exists) return;
        NoticeDO notice = new NoticeDO();
        notice.setTitle("欢迎使用 nz-admin");
        notice.setContent("系统初始化完成，可使用 admin / admin123 登录。");
        notice.setType(2);
        notice.setStatus(0);
        notice.setRemark("系统内置公告");
        noticeService.save(notice);
    }

    private void ensureJob() {
        boolean exists = jobService.listPage(1, 200, null, null, null).getRecords().stream()
                .anyMatch(job -> "系统演示任务".equals(job.getJobName()));
        if (exists) return;
        JobDO job = new JobDO();
        job.setJobName("系统演示任务");
        job.setJobGroup("SYSTEM");
        job.setInvokeTarget("demoTask.run");
        job.setCronExpression("0 0/30 * * * ?");
        job.setConcurrent(1);
        job.setStatus(1);
        job.setRemark("初始化占位任务，默认暂停");
        jobService.save(job);
    }

    private MenuDO createMenu(Long parentId, String name, String path, String component, String icon, int sort, String type, String perm, int visible) {
        MenuDO menu = new MenuDO();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setSort(sort);
        menu.setType(type);
        menu.setPerm(perm);
        menu.setVisible(visible);
        menu.setStatus(0);
        return menu;
    }

    private void saveButtons(Long parentId, String permPrefix, int sortBase, boolean withMutationButtons) {
        List<String[]> buttons = withMutationButtons
                ? Arrays.asList(
                    new String[]{"查询", permPrefix + ":query"},
                    new String[]{"新增", permPrefix + ":add"},
                    new String[]{"修改", permPrefix + ":edit"},
                    new String[]{"删除", permPrefix + ":remove"}
                )
                : List.of(new String[]{"查询", permPrefix + ":query"}, new String[]{"删除", permPrefix + ":remove"});
        for (int i = 0; i < buttons.size(); i++) {
            String[] button = buttons.get(i);
            boolean exists = menuService.listAll().stream()
                    .anyMatch(menu -> Objects.equals(parentId, menu.getParentId()) && button[1].equals(menu.getPerm()));
            if (exists) continue;
            MenuDO btn = createMenu(parentId, button[0], null, null, null, sortBase * 100 + i, "F", button[1], 0);
            menuService.save(btn);
        }
    }
}
