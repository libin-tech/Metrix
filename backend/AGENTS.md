# AGENTS.md — Backend

> 全局通用规范参见：`../AGENTS.md`

# 核心框架
- Java 21
- Spring Boot 4.0.5
- Postgresql
- MyBatis-Plus 3.5.15
- LangChain4j 0.36.2
- Hutool 5.8.13

# 开发规范
## 1. Maven (pom.xml) 规范
- 所有依赖必须指定版本号或通过 dependencyManagement 管理。
- 禁止引入未使用的 starter。

## 2. 分层规范
- 遵循标准 Maven 结构：backend/src/main/java/{package}/{module}。
- controller 层只负责请求转发和参数校验。
- service 层负责业务逻辑。核心业务逻辑在 service 层完成。
- repository 层负责数据库持久化。禁止引入其他持久层框架。禁止处理数据以外的业务。

## 3. MyBatis-Plus 实体规范
- 实体类必须继承 `BaseEntity`。
- 必须使用 Lombok 的 `@Data`, `@AllArgsConstructor` , `@NoArgsConstructor`,`@EqualsAndHashCode(callSuper = true)`,注解。
- 日期时间字段必须使用 `LocalDateTime` 类型。
- 日期字段必须使用 `LocalDate` 类型。
- 时间字段必须使用 `LocalTime` 类型。
- 使用条件构造器方式进行操作。禁止直接写sql。
- 表示状态或类型的字段需要用枚举类代替。枚举类型使用MyBatis-Plus自动映射枚举。
- 所有字段必须添加注释。


## 4. 建表语句规范
- 必须包含 `id`, `create_time`, `update_time`, `version`, `creator`, `modifier` 字段。
- 必须有表注释和字段注释。
- 涉及到表变更的需要完善到 `.doc/db/` 目录下。

## 5. 代码风格
- 核心业务逻辑必须写 JavaDoc 注释。
- 禁止使用 `@SuppressWarnings` 注解。
- 禁止使用 `@Deprecated` 注解。

## 6. 编码规范
- 禁止使用 `@Autowired` 注解，必须使用构造函数注入。
- 枚举值必须使用 `@AllArgsConstructor` 注解。
- 优先使用 `Hutool` 提供的工具类（通用工具、判空等）。
- 涉及到日志打印的使用 `@Slf4j` 注解。
- 禁止使用线程池，必须使用JDK21的虚拟线程。
- 禁止出现硬编码，使用常量代替。常量类存放路径：`com.bintech.metrix.constants`

## 7. 数据库规范
- 脚本存放路径：`.doc/db/V{version}_{description}.sql`

