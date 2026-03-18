package com.example.warningsystem;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class WarningController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AtomicLong taskIdGenerator = new AtomicLong(1000);
    private final List<ControlTask> controlTasks = new CopyOnWriteArrayList<>();
    private final Map<String, StrategyState> strategyStore = new HashMap<>();

    public WarningController() {
        seedData();
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        if (req == null || req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
            return ApiResponse.error("账号和密码不能为空");
        }
        if (!"admin".equals(req.username) || !"admin123".equals(req.password)) {
            return ApiResponse.error("账号或密码错误");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", UUID.randomUUID().toString().replace("-", ""));
        data.put("username", req.username);
        return ApiResponse.success(data);
    }

    @GetMapping("/farm/tree")
    public ApiResponse<List<Map<String, Object>>> farmTree() {
        List<Map<String, Object>> tree = new ArrayList<>();
        tree.add(node(1L, "东部高标农田示范区", "zone", List.of(
                node(11L, "A1 区 (连栋温室)", "greenhouse", List.of(
                        node(111L, "普罗旺斯番茄", "crop", List.of())
                )),
                node(12L, "A2 区 (日光温室)", "greenhouse", List.of(
                        node(121L, "荷兰水果黄瓜", "crop", List.of())
                ))
        )));
        tree.add(node(2L, "南部露天果园基地", "zone", List.of(
                node(21L, "B1 区 (网室)", "greenhouse", List.of(
                        node(211L, "红颜草莓", "crop", List.of())
                ))
        )));
        return ApiResponse.success(tree);
    }

    @GetMapping("/strategy/get")
    public ApiResponse<Map<String, Object>> getStrategy(@RequestParam String target) {
        StrategyState state = strategyStore.computeIfAbsent(target, k -> StrategyState.defaultState());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("strategyStatus", state.strategyStatus);
        data.put("formData", state.formData);
        data.put("estimatedFertilizer", state.estimatedFertilizer);
        return ApiResponse.success(data);
    }

    @PostMapping("/strategy/updateStatus")
    public ApiResponse<Map<String, Object>> updateStatus(@RequestBody UpdateStatusRequest req) {
        if (req == null || req.target == null || req.target.isBlank()) {
            return ApiResponse.error("target 不能为空");
        }
        StrategyState state = strategyStore.computeIfAbsent(req.target, k -> StrategyState.defaultState());
        state.strategyStatus = Boolean.TRUE.equals(req.status);
        return ApiResponse.success(Map.of("success", true));
    }

    @PostMapping("/strategy/issue")
    public ApiResponse<Map<String, Object>> issueStrategy(@RequestBody IssueStrategyRequest req) {
        if (req == null || req.target == null || req.target.isBlank()) {
            return ApiResponse.error("target 不能为空");
        }
        StrategyState state = strategyStore.computeIfAbsent(req.target, k -> StrategyState.defaultState());
        if (req.formData != null) {
            state.formData.putAll(req.formData);
        }
        String jobId = "JOB" + System.currentTimeMillis();
        return ApiResponse.success(Map.of("jobId", jobId));
    }

    @GetMapping("/devices/list")
    public ApiResponse<List<Map<String, Object>>> devices(@RequestParam(required = false) String target) {
        List<Map<String, Object>> rows = List.of(
                device("CTRL-A1-001", "施肥机A主泵控制器", "A1区", "在线", "2026-03-18 10:22:11"),
                device("VALVE-A1-102", "滴灌电磁阀组-01", "A1区(1-5跨)", "在线", "2026-03-18 10:23:05"),
                device("VALVE-A1-103", "滴灌电磁阀组-02", "A1区(6-10跨)", "在线", "2026-03-18 10:21:50"),
                device("SENS-A1-S01", "多深度土壤测控仪", "A1区中心", "在线", "2026-03-18 10:23:12")
        );
        return ApiResponse.success(rows);
    }

    @GetMapping("/forecast/chart")
    public ApiResponse<Map<String, Object>> forecast(@RequestParam(required = false) String target) {
        List<String> dates = new ArrayList<>();
        LocalDate d = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            dates.add(d.plusDays(i).toString());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dates", dates);
        data.put("waterForecast", List.of(28, 30, 25, 0, 35, 32, 28));
        data.put("waterHistory", List.of(30, 32, 28, 10, 38, 35, 30));
        data.put("fertForecast", List.of(12, 14, 10, 0, 16, 15, 12));
        data.put("fertHistory", List.of(15, 16, 13, 5, 18, 17, 14));
        return ApiResponse.success(data);
    }

    @GetMapping("/controlTasks")
    public ApiResponse<Map<String, Object>> listTasks(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) String actionType,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String deviceId,
                                                       @RequestParam(required = false) String zoneName,
                                                       @RequestParam(required = false) String startAt,
                                                       @RequestParam(required = false) String endAt) {
        List<ControlTask> filtered = controlTasks.stream()
                .filter(t -> actionType == null || actionType.isBlank() || actionType.equals(t.actionType))
                .filter(t -> status == null || status.isBlank() || status.equals(t.status))
                .filter(t -> deviceId == null || deviceId.isBlank() || t.deviceId.contains(deviceId))
                .filter(t -> zoneName == null || zoneName.isBlank() || t.zoneName.contains(zoneName))
                .sorted(Comparator.comparing(ControlTask::getId).reversed())
                .collect(Collectors.toList());

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filtered.size(), from + pageSize);
        List<ControlTask> pageList = from >= filtered.size() ? List.of() : filtered.subList(from, to);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", pageList);
        data.put("total", filtered.size());
        return ApiResponse.success(data);
    }

    @PostMapping("/controlTasks")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody SaveTaskRequest req) {
        ControlTask t = new ControlTask();
        t.id = taskIdGenerator.incrementAndGet();
        t.actionType = req.actionType;
        t.deviceId = req.deviceId;
        t.zoneName = req.zoneName;
        t.scheduledAt = req.scheduledAt;
        t.executedAt = null;
        t.operator = "manual";
        t.retryCount = 0;
        t.status = "pending";
        controlTasks.add(t);
        return ApiResponse.success(Map.of("id", t.id));
    }

    @PutMapping("/controlTasks/{id}")
    public ApiResponse<Map<String, Object>> updateTask(@PathVariable Long id, @RequestBody SaveTaskRequest req) {
        ControlTask task = findTask(id);
        if (task == null) {
            return ApiResponse.error("任务不存在");
        }
        task.actionType = req.actionType;
        task.deviceId = req.deviceId;
        task.zoneName = req.zoneName;
        task.scheduledAt = req.scheduledAt;
        if (req.status != null && !req.status.isBlank()) {
            task.status = req.status;
        }
        return ApiResponse.success(Map.of("updated", true));
    }

    @DeleteMapping("/controlTasks/{id}")
    public ApiResponse<Map<String, Object>> deleteTask(@PathVariable Long id) {
        controlTasks.removeIf(t -> Objects.equals(t.id, id));
        return ApiResponse.success(Map.of("deleted", true));
    }

    @PostMapping("/controlTasks/{id}/terminate")
    public ApiResponse<Map<String, Object>> terminateTask(@PathVariable Long id) {
        ControlTask task = findTask(id);
        if (task == null) {
            return ApiResponse.error("任务不存在");
        }
        task.status = "cancelled";
        task.executedAt = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        return ApiResponse.success(Map.of("terminated", true));
    }

    @PostMapping("/controlTasks/{id}/retry")
    public ApiResponse<Map<String, Object>> retryTask(@PathVariable Long id) {
        ControlTask task = findTask(id);
        if (task == null) {
            return ApiResponse.error("任务不存在");
        }
        task.status = "pending";
        task.retryCount = task.retryCount + 1;
        return ApiResponse.success(Map.of("retried", true));
    }

    private ControlTask findTask(Long id) {
        return controlTasks.stream().filter(t -> Objects.equals(t.id, id)).findFirst().orElse(null);
    }

    private void seedData() {
        StrategyState state = StrategyState.defaultState();
        strategyStore.put("A1区-普罗旺斯番茄", state);

        ControlTask t1 = new ControlTask();
        t1.id = taskIdGenerator.incrementAndGet();
        t1.actionType = "irrigation";
        t1.deviceId = "CTRL-A1-001";
        t1.zoneName = "A1区";
        t1.scheduledAt = LocalDateTime.now().minusMinutes(20).format(DATE_TIME_FORMATTER);
        t1.executedAt = LocalDateTime.now().minusMinutes(10).format(DATE_TIME_FORMATTER);
        t1.operator = "system";
        t1.retryCount = 0;
        t1.status = "success";
        controlTasks.add(t1);

        ControlTask t2 = new ControlTask();
        t2.id = taskIdGenerator.incrementAndGet();
        t2.actionType = "fertilize";
        t2.deviceId = "VALVE-A1-102";
        t2.zoneName = "A1区(1-5跨)";
        t2.scheduledAt = LocalDateTime.now().plusMinutes(15).format(DATE_TIME_FORMATTER);
        t2.executedAt = null;
        t2.operator = "manual";
        t2.retryCount = 1;
        t2.status = "failed";
        controlTasks.add(t2);
    }

    private Map<String, Object> device(String id, String name, String area, String status, String lastSync) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deviceId", id);
        row.put("deviceName", name);
        row.put("area", area);
        row.put("status", status);
        row.put("lastSync", lastSync);
        return row;
    }

    private Map<String, Object> node(Long id, String label, String type, List<Map<String, Object>> children) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        row.put("type", type);
        row.put("children", children);
        return row;
    }

    static class UpdateStatusRequest {
        public String target;
        public Boolean status;
    }

    static class IssueStrategyRequest {
        public String target;
        public Map<String, Object> formData;
    }

    static class SaveTaskRequest {
        public String actionType;
        public String deviceId;
        public String zoneName;
        public String scheduledAt;
        public String status;
    }

    static class LoginRequest {
        public String username;
        public String password;
    }

    static class ControlTask {
        public Long id;
        public String actionType;
        public String deviceId;
        public String zoneName;
        public String scheduledAt;
        public String executedAt;
        public String operator;
        public Integer retryCount;
        public String status;

        public Long getId() {
            return id;
        }
    }

    static class StrategyState {
        public boolean strategyStatus;
        public Map<String, Object> formData;
        public double estimatedFertilizer;

        static StrategyState defaultState() {
            StrategyState s = new StrategyState();
            s.strategyStatus = true;
            s.estimatedFertilizer = 24.5;
            s.formData = new LinkedHashMap<>();
            s.formData.put("triggerMetric", "moisture");
            s.formData.put("triggerThreshold", "45");
            s.formData.put("triggerDuration", 2.0);
            s.formData.put("weatherConditions", List.of("无降雨预警", "光照强度>10000Lx"));
            s.formData.put("formulaRatio", "15-5-25");
            s.formData.put("targetEC", 2.2);
            s.formData.put("targetPH", 6.0);
            s.formData.put("irrigationVolume", 8);
            s.formData.put("irrigationDuration", 45);
            s.formData.put("flushDuration", 5);
            return s;
        }
    }
}
