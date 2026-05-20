# task-config-filtering Specification

## Purpose

任务配置 SHALL 支持声明文件大小、文件名和目录名称过滤规则，用于限定 STRM 生成任务实际处理的文件范围。

## Requirements

### Requirement: 任务配置过滤字段

系统 SHALL 在任务配置中保存以下过滤字段：

1. `minFileSizeBytes`
2. `fileNameExcludeRegex`
3. `directoryNameExcludeRegex`

#### Scenario: 创建未配置过滤规则的任务
- **WHEN** 用户创建任务且所有过滤字段为空
- **THEN** 系统 SHALL 保存任务
- **AND** 该任务 SHALL 不启用任务级过滤规则

#### Scenario: 创建配置文件大小过滤的任务
- **WHEN** 用户设置 `minFileSizeBytes`
- **THEN** 系统 SHALL 保存对应字节值

#### Scenario: 创建配置正则过滤的任务
- **WHEN** 用户设置文件名或目录名称正则
- **THEN** 系统 SHALL 保存该正则字符串

---

### Requirement: 任务配置过滤校验

系统 SHALL 在创建和更新任务配置时校验过滤规则：

1. 文件大小边界 SHALL 大于或等于 0
2. 正则字段 SHALL 能被 Java `Pattern.compile()` 编译

#### Scenario: 正则表达式无效
- **WHEN** 任一正则字段无法编译
- **THEN** 系统 SHALL 拒绝保存任务

---

### Requirement: 任务配置接口返回过滤字段

系统 SHALL 在任务配置查询、创建和更新接口中返回过滤字段。

#### Scenario: 查询任务配置
- **WHEN** 用户查询任务配置列表或详情
- **THEN** 响应 SHALL 包含该任务保存的过滤字段
