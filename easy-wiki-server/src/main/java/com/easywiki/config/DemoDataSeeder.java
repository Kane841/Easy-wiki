package com.easywiki.config;

import com.easywiki.entity.ChatMessage;
import com.easywiki.entity.Group;
import com.easywiki.entity.GroupInvite;
import com.easywiki.entity.GroupJoinRequest;
import com.easywiki.entity.GroupMember;
import com.easywiki.entity.Notification;
import com.easywiki.entity.Task;
import com.easywiki.entity.TaskLog;
import com.easywiki.entity.User;
import com.easywiki.entity.UserDevice;
import com.easywiki.entity.WikiPage;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.JoinRequestStatus;
import com.easywiki.enums.MemberRole;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.enums.TaskPriority;
import com.easywiki.enums.TaskStatus;
import com.easywiki.repository.ChatMessageRepository;
import com.easywiki.repository.GroupInviteRepository;
import com.easywiki.repository.GroupJoinRequestRepository;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.GroupRepository;
import com.easywiki.repository.NotificationRepository;
import com.easywiki.repository.TaskLogRepository;
import com.easywiki.repository.TaskRepository;
import com.easywiki.repository.UserDeviceRepository;
import com.easywiki.repository.UserRepository;
import com.easywiki.repository.WikiPageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 演示数据种子：启动时全量覆盖写入，覆盖认证、小组、Wiki、任务、群聊、通知等模块。
 * 启用方式：easywiki.demo.seed=true（见 application-demo.yml）
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "easywiki.demo.seed", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoProperties demoProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final WikiPageRepository wikiPageRepository;
    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;

    public DemoDataSeeder(DemoProperties demoProperties,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository,
                          GroupInviteRepository groupInviteRepository,
                          GroupJoinRequestRepository groupJoinRequestRepository,
                          WikiPageRepository wikiPageRepository,
                          TaskRepository taskRepository,
                          TaskLogRepository taskLogRepository,
                          ChatMessageRepository chatMessageRepository,
                          NotificationRepository notificationRepository,
                          UserDeviceRepository userDeviceRepository) {
        this.demoProperties = demoProperties;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.wikiPageRepository = wikiPageRepository;
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.notificationRepository = notificationRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== Easy-wiki 演示数据初始化 ==========");
        clearAll();
        DemoContext ctx = seedUsers();
        seedGroups(ctx);
        seedGroupMembers(ctx);
        seedInvitesAndJoinRequests(ctx);
        seedWikiPages(ctx);
        seedTasks(ctx);
        seedChatMessages(ctx);
        seedNotifications(ctx);
        seedUserDevices(ctx);
        printAccountSummary(ctx);
        log.info("========== 演示数据写入完成 ==========");
    }

    private void clearAll() {
        taskLogRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        chatMessageRepository.deleteAllInBatch();
        wikiPageRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        groupInviteRepository.deleteAllInBatch();
        groupJoinRequestRepository.deleteAllInBatch();
        groupMemberRepository.deleteAllInBatch();
        userDeviceRepository.deleteAllInBatch();
        groupRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        log.info("已清空现有业务数据");
    }

    private DemoContext seedUsers() {
        DemoContext ctx = new DemoContext();
        String hash = passwordEncoder.encode(demoProperties.getPassword());

        ctx.users.put("zhangwei", saveUser("zhangwei", "zhangwei@xingkong.tech", hash));
        ctx.users.put("lina", saveUser("lina", "lina@xingkong.tech", hash));
        ctx.users.put("wanghao", saveUser("wanghao", "wanghao@xingkong.tech", hash));
        ctx.users.put("chenyu", saveUser("chenyu", "chenyu@xingkong.tech", hash));
        ctx.users.put("zhaojie", saveUser("zhaojie", "zhaojie@outlook.com", hash));
        ctx.users.put("sunqi", saveUser("sunqi", "sunqi@freelance.dev", hash));
        return ctx;
    }

    private User saveUser(String username, String email, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        return userRepository.save(user);
    }

    private void seedGroups(DemoContext ctx) {
        Group product = new Group();
        product.setName("星空科技 · Easy-wiki 产品研发组");
        product.setDescription("负责 Easy-wiki V1.0 的需求、开发、测试与文档协作。可在「发现小组」中搜索加入。");
        product.setDiscoverable(true);
        product.setCreatedBy(ctx.userId("zhangwei"));
        product.setCreatedAt(LocalDateTime.now().minusDays(28));
        ctx.groups.put("product", groupRepository.save(product));

        Group opensource = new Group();
        opensource.setName("Easy-wiki 开源社区");
        opensource.setDescription("对外开源文档、贡献指南与版本发布说明。");
        opensource.setDiscoverable(true);
        opensource.setCreatedBy(ctx.userId("zhangwei"));
        opensource.setCreatedAt(LocalDateTime.now().minusDays(20));
        ctx.groups.put("opensource", groupRepository.save(opensource));

        Group qa = new Group();
        qa.setName("内部 QA 保密组");
        qa.setDescription("预发布回归测试记录，不对外公开。");
        qa.setDiscoverable(false);
        qa.setCreatedBy(ctx.userId("wanghao"));
        qa.setCreatedAt(LocalDateTime.now().minusDays(10));
        ctx.groups.put("qa", groupRepository.save(qa));
    }

    private void seedGroupMembers(DemoContext ctx) {
        addMember(ctx.groupId("product"), ctx.userId("zhangwei"), MemberRole.ADMIN, 28);
        addMember(ctx.groupId("product"), ctx.userId("lina"), MemberRole.MEMBER, 27);
        addMember(ctx.groupId("product"), ctx.userId("wanghao"), MemberRole.MEMBER, 26);
        addMember(ctx.groupId("product"), ctx.userId("chenyu"), MemberRole.MEMBER, 25);

        addMember(ctx.groupId("opensource"), ctx.userId("zhangwei"), MemberRole.ADMIN, 20);
        addMember(ctx.groupId("opensource"), ctx.userId("lina"), MemberRole.MEMBER, 19);

        addMember(ctx.groupId("qa"), ctx.userId("wanghao"), MemberRole.ADMIN, 10);
    }

    private void addMember(Long groupId, Long userId, MemberRole role, int daysAgo) {
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(LocalDateTime.now().minusDays(daysAgo));
        groupMemberRepository.save(member);
    }

    private void seedInvitesAndJoinRequests(DemoContext ctx) {
        GroupInvite validInvite = new GroupInvite();
        validInvite.setGroupId(ctx.groupId("product"));
        validInvite.setToken("demo-invite-product-2026");
        validInvite.setExpiresAt(LocalDateTime.now().plusDays(7));
        validInvite.setCreatedBy(ctx.userId("zhangwei"));
        validInvite.setCreatedAt(LocalDateTime.now().minusDays(1));
        groupInviteRepository.save(validInvite);
        ctx.demoInviteToken = validInvite.getToken();

        GroupInvite opensourceInvite = new GroupInvite();
        opensourceInvite.setGroupId(ctx.groupId("opensource"));
        opensourceInvite.setToken(UUID.randomUUID().toString());
        opensourceInvite.setExpiresAt(LocalDateTime.now().plusDays(14));
        opensourceInvite.setCreatedBy(ctx.userId("zhangwei"));
        opensourceInvite.setCreatedAt(LocalDateTime.now().minusDays(2));
        groupInviteRepository.save(opensourceInvite);

        GroupJoinRequest pending = new GroupJoinRequest();
        pending.setGroupId(ctx.groupId("product"));
        pending.setUserId(ctx.userId("zhaojie"));
        pending.setReason("我是前端工程师，希望参与 Wiki 编辑器与任务看板的体验优化。");
        pending.setStatus(JoinRequestStatus.PENDING);
        pending.setCreatedAt(LocalDateTime.now().minusHours(6));
        groupJoinRequestRepository.save(pending);

        GroupJoinRequest rejected = new GroupJoinRequest();
        rejected.setGroupId(ctx.groupId("product"));
        rejected.setUserId(ctx.userId("sunqi"));
        rejected.setReason("想了解一下项目架构。");
        rejected.setStatus(JoinRequestStatus.REJECTED);
        rejected.setCreatedAt(LocalDateTime.now().minusDays(5));
        groupJoinRequestRepository.save(rejected);

        GroupJoinRequest expired = new GroupJoinRequest();
        expired.setGroupId(ctx.groupId("opensource"));
        expired.setUserId(ctx.userId("sunqi"));
        expired.setReason("计划贡献 Android 客户端文档。");
        expired.setStatus(JoinRequestStatus.EXPIRED);
        expired.setCreatedAt(LocalDateTime.now().minusDays(10));
        groupJoinRequestRepository.save(expired);
    }

    private void seedWikiPages(DemoContext ctx) {
        Long groupId = ctx.groupId("product");
        Long zhangwei = ctx.userId("zhangwei");

        WikiPage onboarding = saveWiki(groupId, null, "新人 onboarding 指南", wikiOnboarding(), 0, zhangwei, 7);
        ctx.wikiPages.put("onboarding", onboarding.getId());

        WikiPage productDoc = saveWiki(groupId, null, "产品文档", null, 1, zhangwei, 14);
        saveWiki(groupId, productDoc.getId(), "V1.0 需求说明书", wikiPrd(), 0, zhangwei, 12);
        saveWiki(groupId, productDoc.getId(), "竞品分析：Notion vs 飞书文档", wikiCompetitive(), 1, ctx.userId("chenyu"), 10);

        WikiPage techDoc = saveWiki(groupId, null, "技术 Wiki", null, 2, ctx.userId("lina"), 13);
        saveWiki(groupId, techDoc.getId(), "后端 API 设计规范", wikiApiDesign(), 0, ctx.userId("lina"), 11);
        saveWiki(groupId, techDoc.getId(), "Android 客户端架构说明", wikiAndroidArch(), 1, ctx.userId("wanghao"), 9);

        saveWiki(groupId, null, "Sprint 15 站会纪要", wikiStandup(), 3, zhangwei, 2);

        saveWiki(ctx.groupId("opensource"), null, "贡献指南", wikiContributing(), 0, zhangwei, 8);
        saveWiki(ctx.groupId("qa"), null, "V1.0 回归测试清单", wikiQaChecklist(), 0, ctx.userId("wanghao"), 3);
    }

    private WikiPage saveWiki(Long groupId, Long parentId, String title, String content,
                              int sortOrder, Long createdBy, int daysAgo) {
        WikiPage page = new WikiPage();
        page.setGroupId(groupId);
        page.setParentId(parentId);
        page.setTitle(title);
        page.setContent(content);
        page.setSortOrder(sortOrder);
        page.setVersion(0);
        page.setCreatedBy(createdBy);
        page.setUpdatedAt(LocalDateTime.now().minusDays(daysAgo));
        return wikiPageRepository.save(page);
    }

    private void seedTasks(DemoContext ctx) {
        Long groupId = ctx.groupId("product");
        Long zhangwei = ctx.userId("zhangwei");
        Long lina = ctx.userId("lina");
        Long wanghao = ctx.userId("wanghao");
        Long chenyu = ctx.userId("chenyu");

        Task doneDoc = saveTask(groupId, "完善 Wiki 乐观锁机制文档", taskWikiLockDoc(),
                TaskStatus.DONE, TaskPriority.HIGH, lina, AssignmentStatus.ACCEPTED,
                zhangwei, LocalDateTime.now().minusDays(3), 14);
        ctx.tasks.put("doneDoc", doneDoc.getId());
        saveTaskLog(doneDoc.getId(), "CREATE", zhangwei, null, TaskStatus.IN_PROGRESS,
                null, AssignmentStatus.ACCEPTED, 14);
        saveTaskLog(doneDoc.getId(), "STATUS_CHANGE", lina, TaskStatus.IN_PROGRESS, TaskStatus.DONE,
                AssignmentStatus.ACCEPTED, AssignmentStatus.ACCEPTED, 3);

        Task inProgress = saveTask(groupId, "Android 任务看板拖拽排序", taskKanbanDrag(),
                TaskStatus.IN_PROGRESS, TaskPriority.URGENT, wanghao, AssignmentStatus.ACCEPTED,
                zhangwei, LocalDateTime.now().plusDays(2), 8);
        ctx.tasks.put("inProgress", inProgress.getId());
        saveTaskLog(inProgress.getId(), "CREATE", zhangwei, null, TaskStatus.IN_PROGRESS,
                null, AssignmentStatus.ACCEPTED, 8);

        Task pendingAccept = saveTask(groupId, "设计新版通知中心 UI", taskNotificationUi(),
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, chenyu, AssignmentStatus.PENDING_ACCEPT,
                zhangwei, LocalDateTime.now().plusDays(5), 4);
        ctx.tasks.put("pendingAccept", pendingAccept.getId());

        saveTask(groupId, "集成 DeepSeek Agent 对话流", taskAgentIntegration(),
                TaskStatus.TODO, TaskPriority.HIGH, null, AssignmentStatus.UNASSIGNED,
                zhangwei, LocalDateTime.now().plusDays(10), 6);

        Task justAssigned = saveTask(groupId, "编写 E2E 验收测试用例", taskE2eCases(),
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, lina, AssignmentStatus.PENDING_ACCEPT,
                zhangwei, LocalDateTime.now().plusDays(7), 1);
        ctx.tasks.put("justAssigned", justAssigned.getId());

        Task overdue = saveTask(groupId, "修复群聊 @ 提及通知偶发丢失", taskMentionBug(),
                TaskStatus.TODO, TaskPriority.URGENT, lina, AssignmentStatus.ACCEPTED,
                wanghao, LocalDateTime.now().minusDays(1), 5);
        ctx.tasks.put("overdue", overdue.getId());

        saveTask(ctx.groupId("opensource"), "撰写 README 快速上手指南", "补充 Docker 与本地 MySQL 两种部署方式对比。",
                TaskStatus.TODO, TaskPriority.LOW, null, AssignmentStatus.UNASSIGNED,
                zhangwei, null, 3);

        saveTask(ctx.groupId("qa"), "V1.0 发布前全量回归", "覆盖 Wiki / 任务 / 群聊 / 通知 / Agent 五个 Tab。",
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, wanghao, AssignmentStatus.ACCEPTED,
                wanghao, LocalDateTime.now().plusDays(1), 2);
    }

    private Task saveTask(Long groupId, String title, String description,
                          TaskStatus status, TaskPriority priority, Long assigneeId,
                          AssignmentStatus assignmentStatus, Long creatorId,
                          LocalDateTime dueDate, int daysAgo) {
        Task task = new Task();
        task.setGroupId(groupId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setAssigneeId(assigneeId);
        task.setAssignmentStatus(assignmentStatus);
        task.setCreatorId(creatorId);
        task.setDueDate(dueDate);
        task.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return taskRepository.save(task);
    }

    private void saveTaskLog(Long taskId, String action, Long operatorId,
                             TaskStatus fromStatus, TaskStatus toStatus,
                             AssignmentStatus fromAssignment, AssignmentStatus toAssignment,
                             int daysAgo) {
        TaskLog logEntry = new TaskLog();
        logEntry.setTaskId(taskId);
        logEntry.setAction(action);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setFromAssignment(fromAssignment);
        logEntry.setToAssignment(toAssignment);
        logEntry.setOperatorId(operatorId);
        logEntry.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        taskLogRepository.save(logEntry);
    }

    private void seedChatMessages(DemoContext ctx) {
        Long groupId = ctx.groupId("product");
        Long zhangwei = ctx.userId("zhangwei");
        Long lina = ctx.userId("lina");
        Long wanghao = ctx.userId("wanghao");
        Long chenyu = ctx.userId("chenyu");

        saveChat(groupId, zhangwei, "大家早上好，Sprint 15 今天正式进入开发阶段，Wiki 和任务看板记得及时更新。", null, 3, 9, 30);
        saveChat(groupId, lina, "收到，后端乐观锁文档我已经补到「技术 Wiki」里了，@zhangwei 帮忙 review 一下。",
                mentionJson(zhangwei), 3, 9, 45);
        saveChat(groupId, wanghao, "@lina API 文档里任务状态枚举和 Android 端对不上，我这边先按 TODO/IN_PROGRESS/DONE 实现了。",
                mentionJson(lina), 3, 10, 5);
        saveChat(groupId, chenyu, "通知中心新版稿子在 Figma 了，@zhangwei 有空看下信息层级是否符合预期。",
                mentionJson(zhangwei), 2, 15, 20);
        saveChat(groupId, zhangwei, "@chenyu 整体不错，角标未读数和分组折叠再优化一版。另外 @wanghao 拖拽排序 demo 周五前能给一版吗？",
                mentionJson(chenyu, wanghao), 2, 15, 35);
        saveChat(groupId, lina, "赵杰提交了入组申请，我看背景是前端，要不要拉进来看看 Wiki 编辑器体验？", null, 1, 11, 0);
        saveChat(groupId, wanghao, "可以，正好缺人测 Android 端 Markdown 渲染。", null, 1, 11, 8);
        saveChat(groupId, zhangwei, "好，我先审批。顺便提醒：@lina 那个 @ 通知丢失的 bug 优先级提到 URGENT 了。",
                mentionJson(lina), 0, 14, 10);

        saveChat(ctx.groupId("opensource"), zhangwei, "开源仓库 README 还需要补演示账号说明，@lina 你来起草？",
                mentionJson(ctx.userId("lina")), 1, 16, 0);
    }

    private String mentionJson(Long... userIds) {
        if (userIds.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < userIds.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(userIds[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private void saveChat(Long groupId, Long senderId, String content, String mentions,
                          int daysAgo, int hour, int minute) {
        ChatMessage message = new ChatMessage();
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMentions(mentions);
        message.setSentAt(LocalDateTime.now().minusDays(daysAgo).withHour(hour).withMinute(minute).withSecond(0));
        chatMessageRepository.save(message);
    }

    private void seedNotifications(DemoContext ctx) {
        Long productGroup = ctx.groupId("product");
        Long zhangwei = ctx.userId("zhangwei");
        Long lina = ctx.userId("lina");
        Long wanghao = ctx.userId("wanghao");
        Long chenyu = ctx.userId("chenyu");

        saveNotification(zhangwei, productGroup, NotificationEventType.JOIN_REQUEST,
                "新的入组申请", "zhaojie 申请加入「星空科技 · Easy-wiki 产品研发组」",
                false, 0, 6, "/groups/" + productGroup + "/members/requests");

        saveNotification(lina, productGroup, NotificationEventType.WIKI_UPDATED,
                "Wiki 页面已更新", "zhangwei 更新了「新人 onboarding 指南」",
                true, 2, 10, "/groups/" + productGroup + "/wiki/" + ctx.wikiPages.get("onboarding"));

        saveNotification(chenyu, productGroup, NotificationEventType.TASK_ASSIGNED,
                "任务指派", "zhangwei 将任务「设计新版通知中心 UI」指派给你",
                false, 1, 9, "/groups/" + productGroup + "/tasks/" + ctx.tasks.get("pendingAccept"));

        saveNotification(zhangwei, productGroup, NotificationEventType.TASK_ACCEPTED,
                "任务已接取", "wanghao 已接取任务「Android 任务看板拖拽排序」",
                true, 2, 14, "/groups/" + productGroup + "/tasks/" + ctx.tasks.get("inProgress"));

        saveNotification(lina, productGroup, NotificationEventType.TASK_ASSIGNED,
                "任务指派", "zhangwei 将任务「编写 E2E 验收测试用例」指派给你",
                false, 0, 10, "/groups/" + productGroup + "/tasks/" + ctx.tasks.get("justAssigned"));

        saveNotification(lina, productGroup, NotificationEventType.TASK_DUE_REMINDER,
                "任务即将到期", "「修复群聊 @ 提及通知偶发丢失」将于明天截止，请尽快处理",
                false, 0, 8, "/groups/" + productGroup + "/tasks/" + ctx.tasks.get("overdue"));

        saveNotification(wanghao, productGroup, NotificationEventType.CHAT_MENTION,
                "群聊 @ 提及", "zhangwei 在群聊中提到了你：好，我先审批。顺便提醒：@lina 那个 @ 通知丢失的 bug...",
                false, 0, 14, "/groups/" + productGroup + "/chat");

        saveNotification(chenyu, productGroup, NotificationEventType.CHAT_MENTION,
                "群聊 @ 提及", "zhangwei 在群聊中提到了你：@chenyu 整体不错，角标未读数和分组折叠再优化一版...",
                true, 2, 15, "/groups/" + productGroup + "/chat");

        saveNotification(ctx.userId("sunqi"), productGroup, NotificationEventType.JOIN_REJECTED,
                "入组申请已被拒绝", "你加入「星空科技 · Easy-wiki 产品研发组」的申请未通过",
                true, 5, 12, null);

        saveNotification(lina, ctx.groupId("opensource"), NotificationEventType.WIKI_UPDATED,
                "Wiki 页面已更新", "zhangwei 更新了「贡献指南」",
                false, 3, 11, "/groups/" + ctx.groupId("opensource") + "/wiki");
    }

    private void saveNotification(Long userId, Long groupId, NotificationEventType type,
                                  String title, String body, boolean read,
                                  int daysAgo, int hour, String targetUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setGroupId(groupId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRead(read);
        notification.setTargetUrl(targetUrl);
        notification.setCreatedAt(LocalDateTime.now().minusDays(daysAgo).withHour(hour).withMinute(0));
        notificationRepository.save(notification);
    }

    private void seedUserDevices(DemoContext ctx) {
        saveDevice(ctx.userId("zhangwei"), "demo-fcm-token-zhangwei-android", "android");
        saveDevice(ctx.userId("lina"), "demo-fcm-token-lina-android", "android");
    }

    private void saveDevice(Long userId, String token, String platform) {
        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setFcmToken(token);
        device.setPlatform(platform);
        device.setUpdatedAt(LocalDateTime.now().minusDays(1));
        userDeviceRepository.save(device);
    }

    private void printAccountSummary(DemoContext ctx) {
        String password = demoProperties.getPassword();
        log.info("");
        log.info("┌─────────────────────────────────────────────────────────────┐");
        log.info("│ 演示账号（密码均为 {}）", password);
        log.info("├──────────┬──────────────────────────┬──────────────────────────┤");
        log.info("│ 用户名   │ 邮箱                     │ 角色说明                 │");
        log.info("├──────────┼──────────────────────────┼──────────────────────────┤");
        log.info("│ zhangwei │ zhangwei@xingkong.tech   │ 产品组长，产品研发组管理员 │");
        log.info("│ lina     │ lina@xingkong.tech       │ 后端工程师               │");
        log.info("│ wanghao  │ wanghao@xingkong.tech    │ Android 工程师           │");
        log.info("│ chenyu   │ chenyu@xingkong.tech     │ UI 设计师                │");
        log.info("│ zhaojie  │ zhaojie@outlook.com      │ 外部用户，有待审批入组申请 │");
        log.info("│ sunqi    │ sunqi@freelance.dev      │ 外部用户，含历史申请记录   │");
        log.info("└──────────┴──────────────────────────┴──────────────────────────┘");
        log.info("");
        log.info("演示小组：");
        log.info("  [1] 星空科技 · Easy-wiki 产品研发组（可发现）— Wiki / 任务 / 群聊 / 通知全覆盖");
        log.info("  [2] Easy-wiki 开源社区（可发现）");
        log.info("  [3] 内部 QA 保密组（不可发现，仅 wanghao）");
        log.info("");
        log.info("邀请链接 token（产品研发组）: {}", ctx.demoInviteToken);
        log.info("  加入接口: POST /api/v1/groups/join/{}", ctx.demoInviteToken);
        log.info("");
    }

    private static String wikiOnboarding() {
        return """
                # 新人 onboarding 指南

                欢迎加入 **星空科技 · Easy-wiki 产品研发组**！

                ## 第一天

                1. 使用演示账号登录 Android 客户端（或 curl 调 API）
                2. 阅读「产品文档 → V1.0 需求说明书」
                3. 在任务看板认领一个 TODO 任务

                ## 协作约定

                - Wiki 更新后组员会收到通知
                - 任务指派后需先「接取」再开始开发
                - 群聊支持 `@用户名` 提及，被 @ 的人会收到通知

                ## 常用链接

                - 健康检查: `GET /api/v1/health`
                - 我的任务: `GET /api/v1/my/tasks`
                """;
    }

    private static String wikiPrd() {
        return """
                # V1.0 需求说明书

                ## 产品定位

                轻量化团队知识库与工作协同平台，面向 5–20 人小团队。

                ## 核心模块

                | 模块 | 说明 | 优先级 |
                |------|------|--------|
                | Wiki | 树形文档、Markdown、乐观锁 | P0 |
                | 任务看板 | 三列状态、指派/接取状态机 | P0 |
                | 群聊 | 实时消息、@ 提及 | P0 |
                | 通知 | 站内 + FCM 推送 | P0 |
                | Agent | DeepSeek 辅助写 Wiki/建任务 | P1 |

                ## 里程碑

                - M1: 后端 API + JWT 认证 ✅
                - M2: Android 五 Tab 工作区 ✅
                - M3: 演示数据 + E2E 验收 ⏳
                """;
    }

    private static String wikiCompetitive() {
        return """
                # 竞品分析：Notion vs 飞书文档

                ## 对比维度

                **Notion**
                - 优势：块编辑器灵活、模板丰富
                - 劣势：国内访问不稳定、小团队成本高

                **飞书文档**
                - 优势：与 IM/日历深度集成
                - 劣势：功能过重，小团队学习成本高

                ## Easy-wiki 差异化

                - 专注 Wiki + 任务 + 群聊三位一体
                - 私有化部署友好
                - 内置 AI Agent 降低文档维护成本
                """;
    }

    private static String wikiApiDesign() {
        return """
                # 后端 API 设计规范

                ## 通用约定

                - Base URL: `/api/v1`
                - 认证: `Authorization: Bearer <JWT>`
                - 响应格式: `{ "code": 0, "message": "ok", "data": ... }`

                ## Wiki 乐观锁

                更新 Wiki 页面时需携带 `version` 字段，冲突返回 409。

                ```json
                PUT /groups/{groupId}/wiki/pages/{pageId}
                { "title": "...", "content": "...", "version": 2 }
                ```
                """;
    }

    private static String wikiAndroidArch() {
        return """
                # Android 客户端架构说明

                ## 技术栈

                - Kotlin + Jetpack Compose
                - Retrofit + OkHttp
                - DataStore（仅存 JWT 与服务器地址）

                ## 模块划分

                ```
                ui/          → Compose 页面
                data/        → Repository + API
                model/       → DTO
                ```

                业务数据不落本地 SQLite，全部走后端 API。
                """;
    }

    private static String wikiStandup() {
        return """
                # Sprint 15 站会纪要

                **日期**: 2026-07-09

                ## 昨日完成

                - @lina: Wiki 乐观锁文档
                - @wanghao: 任务看板基础 UI

                ## 今日计划

                - @chenyu: 通知中心 UI 第二稿
                - @wanghao: 拖拽排序 POC

                ## 阻塞项

                - @ 通知偶发丢失（已建 URGENT 任务跟踪）
                """;
    }

    private static String wikiContributing() {
        return """
                # 贡献指南

                1. Fork 仓库并创建 feature 分支
                2. 后端: `cd easy-wiki-server && mvn test`
                3. Android: `cd easy-wiki-android && ./gradlew test`
                4. 提交 PR 并关联 Issue

                演示数据可通过 `spring.profiles.active=demo` 一键导入。
                """;
    }

    private static String wikiQaChecklist() {
        return """
                # V1.0 回归测试清单

                - [ ] 登录 / 注册
                - [ ] 创建小组 / 邀请加入 / 入组申请审批
                - [ ] Wiki CRUD + 乐观锁冲突
                - [ ] 任务创建 / 指派 / 接取 / 状态流转
                - [ ] 群聊 + @ 提及
                - [ ] 通知列表 + 已读
                - [ ] Agent 对话（需配置 API Key）
                """;
    }

    private static String taskWikiLockDoc() {
        return """
                补充 Wiki 乐观锁的使用说明与冲突处理流程，包括：
                - version 字段语义
                - 409 冲突时客户端提示文案
                - 并发编辑场景测试用例
                """;
    }

    private static String taskKanbanDrag() {
        return """
                在 Android 任务看板实现拖拽排序，要求：
                1. 同一列内可 reorder
                2. 跨列拖拽触发状态变更 API
                3. 动画流畅，松手后回弹自然
                """;
    }

    private static String taskNotificationUi() {
        return """
                设计通知中心新版 UI：
                - 按类型分组（任务 / Wiki / 群聊 / 入组）
                - 未读角标
                - 左滑标记已读
                """;
    }

    private static String taskAgentIntegration() {
        return """
                集成 DeepSeek Agent：
                - 支持自然语言创建 Wiki 页面
                - 支持自然语言创建任务
                - 对话上下文携带当前小组 Wiki/任务摘要
                """;
    }

    private static String taskE2eCases() {
        return """
                基于 docs/E2E-CHECKLIST.md 编写可执行的 curl 脚本，
                覆盖注册、入组、Wiki、任务、群聊、通知全链路。
                """;
    }

    private static String taskMentionBug() {
        return """
                复现步骤：
                1. A 在群聊发送 @B 消息
                2. B 离线时偶发收不到 CHAT_MENTION 通知

                需排查 WebSocket 推送与 FCM  fallback 逻辑。
                """;
    }

    private static class DemoContext {
        final Map<String, User> users = new LinkedHashMap<>();
        final Map<String, Group> groups = new LinkedHashMap<>();
        final Map<String, Long> wikiPages = new LinkedHashMap<>();
        final Map<String, Long> tasks = new LinkedHashMap<>();
        String demoInviteToken;

        Long userId(String key) {
            return users.get(key).getId();
        }

        Long groupId(String key) {
            return groups.get(key).getId();
        }
    }
}
