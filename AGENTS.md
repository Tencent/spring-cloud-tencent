# AGENTS.md

本文件为在本仓库中工作的编码代理提供指导。修改代码前，先理解项目边界、构建方式和既有约定。

## 项目简介

Spring Cloud Tencent 是腾讯开源的微服务解决方案，基于 Spring Cloud 标准 SPI 集成 [Polaris](https://github.com/polarismesh/polaris)（腾讯开源服务治理平台），提供服务注册与发现、动态配置、限流、熔断、流量路由、元数据透传和优雅上下线等能力。

项目按功能拆分为多个 Starter，核心客户端能力由 [polaris-java](https://github.com/polarismesh/polaris-java) 提供。

## 通用规范

- 所有对话和回复使用中文。
- 变量名、分支名和提交信息使用英文。
- 源码注释使用简单英文，代码中不允许出现中文。
- 改动应限定在需求范围内，不顺手重构无关代码，不修改无关文件中的用户改动。

## 参考文档

- [Polaris OpenAPI](https://polarismesh-openapi.apifox.cn/)
- [Polaris](https://github.com/polarismesh/polaris)
- [polaris-java](https://github.com/polarismesh/polaris-java)
- [项目文档](https://github.com/Tencent/spring-cloud-tencent/wiki)

## 方案设计工作流

**大型需求开发或非平凡的缺陷修复在动手写代码前必须先出方案。** 触发条件（满足任一即视为「大型」）：

- 跨 2 个及以上模块的改动
- 新增 Starter、新增 API、改动 SPI 或公开接口
- 涉及并发模型、缓存、连接器协议、版本兼容等架构面影响
- 缺陷根因不清晰、需要先调研复现路径

流程：

1. 用当前工具可用的方案设计手段（skill、subagent 或手动流程均可）完成方案设计：
   - 需求探索：澄清用户意图、需求边界与设计空间（创意/新功能场景）
   - 根因定位：系统化定位缺陷根因（疑难缺陷场景）
   - 计划编写：把方案写成可执行的多步实施计划
2. 方案文档及相关材料保存到 `.agents/specs/<YYYY-MM-DD-short-topic>/` 子目录下，按需求或缺陷修复一个子目录隔离：
   - `.agents/specs/<YYYY-MM-DD-short-topic>/design.md` — 主设计文档，包含：背景、目标、非目标、方案对比、最终选型、影响面、实施步骤、回滚策略、测试计划
   - 同目录下放置该需求相关的其他材料：调研笔记、时序图、抓包/日志样本、验证代码片段、参考资料链接等
   - 注：`.agents/specs/` 仅用于本地 / AI 工作目录，不提交到远程仓库
3. 与用户确认方案后，再进入实现阶段：按计划的步骤逐步推进，或采用测试驱动（先写失败测试，再写实现）。

小改动（拼写错误、单文件局部修复、纯文档调整）可以直接实现。修复缺陷时先编写能复现问题的测试，确认测试失败后再修复。

## 构建与测试命令

仓库包含 `mvnw`，`./mvnw` 与 `mvn` 均可使用。以下命令以 `mvn` 为例。

```bash
# 构建整个项目
mvn package -DskipTests

# 构建整个项目并运行测试
mvn clean package

# 构建单个模块（含依赖）
mvn compile -pl <模块路径> -am -DskipTests -Dmaven.javadoc.skip=true

# 仅构建 quickstart-example 相关模块（开发示例时最快）
mvn -f pom.xml package -pl \
  "spring-cloud-tencent-examples/quickstart-example/quickstart-callee-service-a,\
spring-cloud-tencent-examples/quickstart-example/quickstart-callee-service-b,\
spring-cloud-tencent-examples/quickstart-example/quickstart-caller-service,\
spring-cloud-tencent-examples/quickstart-example/quickstart-gateway-service" \
  -am -DskipTests -Dmaven.javadoc.skip=true

# 运行单个测试类
# -Dtest 是全局参数，先用 -DskipTests 构建依赖，再单独运行目标模块
mvn test -pl <模块路径> -am -DskipTests -Dmaven.javadoc.skip=true && \
mvn test -pl <模块路径> -Dtest=ClassName -Dmaven.javadoc.skip=true

# CI 使用的构建命令
mvn clean test -B -U -Psonatype
```

本地校验代码风格只需要跑编译，不要单独执行 `mvn checkstyle:check`。项目已将 `maven-checkstyle-plugin` 的 `checkstyle-validation` 执行项绑定到 Maven `validate` 阶段，运行 `mvn compile` 或 `mvn test-compile` 时就会自动触发 Checkstyle，并且只关注本次改动引入的问题。单独执行 `checkstyle:check` 会把仓库中既有的、与本次改动无关的风格问题全部报出来。

### 版本号

根 `pom.xml` 通过 `<revision>` 属性统一管理所有子模块版本。版本号以当前分支根 `pom.xml` 的 `<revision>` 为准，不要从其他分支复制。`flatten-maven-plugin` 会生成 `.flattened-pom.xml`，该文件由构建自动更新，**禁止手工编辑**。改版本号只需修改根 `pom.xml` 的 `<revision>`。

#### 移除 `-SNAPSHOT` 前检查

将根 `pom.xml` 的 `<revision>` 从 `-SNAPSHOT` 去掉属于发布动作。执行前必须确认当前分支与 GitHub 默认分支（主分支）的提交一致：

1. 确认工作区干净：

   ```bash
   git status --short
   ```

   有输出时先处理未提交改动，或向用户确认如何处理。

2. 确定主分支名称：

   ```bash
   gh repo view Tencent/spring-cloud-tencent \
     --json defaultBranchRef \
     --jq '.defaultBranchRef.name'
   ```

   不要假设主分支一定叫 `main`。如果本地没有 `gh`、未登录或无法访问 GitHub，则打开 `https://github.com/Tencent/spring-cloud-tencent` 查看默认分支，或直接向用户确认。

3. 获取主分支并比较差异（`<remote>` 替换为指向 `Tencent/spring-cloud-tencent` 的远端）：

   ```bash
   git fetch <remote> <default-branch>
   git log --oneline HEAD..FETCH_HEAD
   git log --oneline FETCH_HEAD..HEAD
   ```

   两条 `git log` 只要有一条输出非空，就说明当前分支与主分支存在不同提交。

4. 如有差异，先向用户列出差异并确认处理方式；确认前不要去掉 `-SNAPSHOT`。

5. 确认一致后，修改根 `pom.xml` 的 `<revision>`，并检查 `git diff`，确保只改动了预期内容。

当前各分支的目标是与 GitHub 主分支保持一致，因此该检查不能省略。

## CI 检查项

PR 提交后 GitHub Actions 会执行以下检查：

- **JUnit 测试**：在 Ubuntu 与 Windows 上运行，JDK 矩阵以当前分支 `.github/workflows/junit.yml` 为准。
- **Checkstyle**：`maven-checkstyle-plugin`，`failsOnError=true`，`failsOnViolation=true`，包含测试源码。
- **许可证文件头**：Apache SkyWalking Eyes 检查所有 Java 源文件头部的许可证声明。
- **Codecov**：上传代码覆盖率。

## 架构概览

### 模块结构

项目为 Maven 多模块工程，按功能拆分。以下为主要模块，具体以当前分支根 `pom.xml` 为准：

**核心与公共模块：**

- `spring-cloud-tencent-commons` — 公共工具、配置与 polaris-java SDK 初始化。
- `spring-cloud-tencent-polaris-context` — Polaris 上下文生命周期管理。
- `spring-cloud-tencent-rpc-enhancement` — 远程过程调用增强。
- `spring-cloud-tencent-dependencies` — BOM，下游接入时统一管理版本。
- `spring-cloud-starter-tencent-all` — 聚合依赖。

**功能 Starter：**

- `spring-cloud-starter-tencent-polaris-discovery` — 服务注册与发现。
- `spring-cloud-starter-tencent-polaris-config` — 动态配置。
- `spring-cloud-starter-tencent-polaris-ratelimit` — 限流。
- `spring-cloud-starter-tencent-polaris-circuitbreaker` — 熔断。
- `spring-cloud-starter-tencent-polaris-router` — 流量路由。
- `spring-cloud-starter-tencent-polaris-contract` — 接口契约。
- `spring-cloud-starter-tencent-polaris-auth` — 认证鉴权。
- `spring-cloud-starter-tencent-metadata-transfer` — 元数据透传。

**可选插件：**

- `spring-cloud-tencent-plugin-starters` 下包含网关、优雅上下线、故障容错、多注册中心、线程上下文、链路追踪、安全等可选插件；具体模块以当前分支的 `spring-cloud-tencent-plugin-starters/pom.xml` 为准。

**其他：**

- `spring-cloud-tencent-examples` — 各功能可运行示例。
- `spring-cloud-tencent-coverage` — 测试覆盖率聚合。

自动装配入口可能是 `META-INF/spring.factories` 或 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，以当前分支实际文件为准。

### 与 polaris-java 的关系

polaris-java 提供核心客户端逻辑，Spring Cloud Tencent 用 Spring 抽象层封装它。关键模块对应关系：

- 服务注册与注销 → polaris-java 的 [polaris-discovery](https://github.com/polarismesh/polaris-java/tree/main/polaris-discovery) 模块
- 优雅上下线 → polaris-java 的 [polaris-plugins-lossless](https://github.com/polarismesh/polaris-java/tree/main/polaris-plugins/polaris-plugins-lossless) 模块
- 配置管理 → polaris-java 的 [polaris-configuration](https://github.com/polarismesh/polaris-java/tree/main/polaris-configuration) 模块

涉及跨仓库改动时，需要同时检查 polaris-java 与本仓库的接口兼容性和测试覆盖。

### 下游接入方式

下游项目通过 `spring-cloud-tencent-dependencies` BOM 统一管理版本：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.tencent.cloud</groupId>
      <artifactId>spring-cloud-tencent-dependencies</artifactId>
      <version>${spring-cloud-tencent.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

随后按需引入 Starter，例如 `spring-cloud-starter-tencent-polaris-discovery`。

## 快速上手示例

位于 `spring-cloud-tencent-examples/quickstart-example/`，包含以下服务：

- `quickstart-callee-service-a`：服务提供方实例 A，业务端口 48083，管理端口 28083。
- `quickstart-callee-service-b`：服务提供方实例 B，业务端口 48084，管理端口 28084。
- `quickstart-caller-service`：服务调用方，业务端口 48082，管理端口 28082。
- `quickstart-gateway-service`：响应式网关，业务端口 48081，管理端口 28081。

其他示例服务和网关模块以当前分支的 quickstart `pom.xml` 为准。

两个服务提供方实例均以 `QuickstartCalleeService` 注册到 Polaris 命名空间 `default`。

**Polaris 服务端：** HTTP、gRPC 和配置中心 gRPC 端口分别为 8090、8091、8093。示例使用的具体地址、命名空间和配置中心地址见各服务的 `application.yml` 或 `bootstrap.yml`。

### 关键接口

服务提供方：

- `GET /quickstart/callee/sum?value1=X&value2=Y`
- `GET /quickstart/callee/info`

服务调用方：

- `GET /quickstart/caller/feign?value1=X&value2=Y`
- `GET /quickstart/caller/rest`
- `GET /quickstart/caller/webclient`

其他接口以当前分支的控制器代码为准。

### 动态配置

监听配置文件 `config/callee.properties`（分组名为 `QuickstartCalleeService`，命名空间为 `default`）中的配置项：

- `appName` — 通过 `@PolarisConfigKVFileChangeListener` 监听并打印日志。
- `datasource.*` — 通过 `@ConfigurationProperties("datasource")` 绑定。
- 其他配置示例以当前分支的配置类和控制器为准。

### 优雅上下线

实现入口为 `spring-cloud-tencent-plugin-starters/spring-cloud-tencent-lossless-plugin`，通过 `LosslessRegistryAspect` 拦截 `ServiceRegistry.register()`。

管理端点监听在管理端口，`/offline` 仅允许本机调用：

- `GET /readiness` — 已注册返回 200，未注册返回 503。
- `GET /online` — `/readiness` 的废弃别名。
- `GET /offline` — 触发注销，非本机调用返回 403。

启动参数开启方式：

```bash
--spring.cloud.polaris.lossless.enabled=true
--spring.cloud.polaris.lossless.delayRegisterInterval=5000
```

## 代码规范

- **缩进必须全部使用制表符（`tab`），禁止使用空格缩进**，主源码和测试源码均适用。CI 的 `RegexpSinglelineJava` 规则会拒绝前导空格。
- 注释使用简单英文，代码中不允许出现中文。
- 所有 Java 源文件必须包含 BSD 3-Clause 许可证文件头。
- Java 和 Spring 版本以当前分支根 `pom.xml` 为准，禁止跨分支套用高版本 API。
- 遵循 Spring Java Format 风格，格式化校验在 Maven `validate` 阶段自动执行。
- 导入顺序、空白等规则由 Checkstyle 校验，禁止通配符导入和行尾空白。
- 新增代码的注释只解释隐藏约束、易错点和不变量，不复述代码表面行为。
- 修改文件时保留既有注释；重构导致注释被误删时，必须在重构后还原。

## 许可证文件头

所有新增 Java 文件必须以如下内容开头：

```java
/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
```

## Pull Request 规范

- PR 提交到上游仓库 `Tencent/spring-cloud-tencent` 的对应长期分支。
- 修改代码后同步更新相关文档和注释。
- 新增文件必须包含许可证声明。

## Git 提交规范

### Commit Message 格式

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```text
<type>: <subject>
```

常用类型：

- `feat`：新增功能。
- `fix`：修复缺陷。
- `refactor`：重构，不改变外部行为。
- `test`：仅测试代码改动。
- `docs`：仅文档改动。
- `chore`：构建脚本、依赖、杂项改动。

变量名、commit message 必须使用英文。

### 必须包含的元数据

每次 commit **必须**包含尾注。按实际执行提交的 Agent 填写，不要混用：

1. **Signed-off-by**（DCO 签名）：使用 `git commit -s` 自动添加。这是上游 DCO 检查的硬性要求，缺失 PR 无法合并。
2. **Co-Authored-By**：根据本次实际使用的工具填写，二选一，不要同时写两条：
   - **Cursor**：不要手写。Agent 创建 commit 时会自动追加 `Co-Authored-By: Cursor <cursoragent@cursor.com>`。禁止填写 `noreply@anthropic.com`。
   - **Claude Code**：根据实际使用的模型、版本和上下文长度填写：

     ```text
     Co-Authored-By: <模型名> <版本> (<上下文长度>) <noreply@anthropic.com>
     ```

     例如：

     ```text
     Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
     ```

   - **其他工具**（Codex、CodeBuddy 等）：按工具实际生成的署名行填写；不确定时只写工具名。本仓库历史提交中的实际写法：

     ```text
     Co-Authored-By: kimi-k3 <noreply@moonshot.cn>
     Co-Authored-By: Cursor <cursoragent@cursor.com>
     Co-Authored-By: CodeBuddy Code
     ```

     模型名与参数以当次会话实际使用的为准，不要凭记忆填。

### 提交命令模板

为保证 message 多行格式正确，使用 HEREDOC 传入。`git commit -s` 会自动追加 `Signed-off-by`：

```bash
git commit -s -m "$(cat <<'EOF'
docs: short subject in english

Optional body explaining the why.

Co-Authored-By: <工具名/模型名> (<参数>) <noreply 邮箱>
EOF
)"
```

只有单行 message 时也可简写：

```bash
git commit -s -m "fix: avoid null metadata in discovery"
```

## 单元测试规范

### 测试框架

- JUnit 5：核心注解 `@Test`、`@BeforeEach`、`@ExtendWith`。
- `spring-boot-starter-test`。
- Mockito：核心注解 `@Mock`、`@InjectMocks`，核心方法 `when()`、`verify()`。
- 对外部调用、随机数、时间等使用 Mockito 进行模拟。
- 不要在测试代码中重新实现被测试方法。
- 对静态方法使用 `mockStatic` 进行模拟。

### 测试代码规范

- **测试中不要使用特殊的硬编码 IP 地址**（如 `10.0.0.1`），统一使用 `127.0.0.1`，通过不同端口区分不同地址。CI 有硬编码 IP 检测，非回环地址会导致检测不通过。
- 使用 `@DisplayName` 描述测试类和测试方法。
- 断言使用 `org.assertj.core.api.Assertions` 下的方法。
- 属性设置方法调用独立成行，不使用链式调用。
- 测试方法采用准备、执行、断言三段式，并加 Javadoc 说明目的、场景和验证内容。
- 私有属性通过反射辅助方法（`getDeclaredField` 加 `setAccessible(true)`）访问。
- 每种数据类型建立独立测试方法。

### 测试类结构模板

```java
/**
 * Test for {@link TestedClass}.
 *
 * @author {{git的user.name}}
 */
@DisplayName("TestedClass")
class XxxTest {
	private static final String TEST_VALUE = "test";

	private Xxx testInstance;

	@BeforeEach
	void setUp() {
		testInstance = new Xxx();
	}

	/**
	 * Test purpose, scenario, and what to verify.
	 */
	@DisplayName("test case description")
	@Test
	void testMethod() {
		// Arrange
		// Act
		// Assert
	}
}
```

