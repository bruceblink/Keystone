<p align="center">
      <img src="https://img.shields.io/badge/Release-V1.8.0-green.svg" alt="Downloads">
      <img src="https://img.shields.io/badge/JDK-17+-green.svg" alt="Build Status">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="Build Status">
   <img src="https://img.shields.io/badge/Spring%20Boot-2.7-blue.svg" alt="Downloads">
   <a target="_blank" href="https://likanug.top">
   <img src="https://img.shields.io/badge/Author-likanug-ff69b4.svg" alt="Downloads">
 </a>
 <a target="_blank" href="https://likanug.top">
   <img src="https://img.shields.io/badge/Copyright%20-@Agileboot-%23ff3f59.svg" alt="Downloads">
 </a>
 </p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">AgileBoot v2.0.0</h1>

<h4 align="center">基于SpringBoot+Vue3前后端分离的Java快速开发框架</h4>
<p align="center">
</p>

## ⚡平台简介⚡

AgileBoot是一套开源的全栈精简快速开发平台，毫无保留给个人及企业免费使用。本项目的目标是做一款精简可靠，代码风格优良，项目规范的小型开发脚手架。
适合个人开发者的小型项目或者公司内部项目使用。也可作为供初学者学习使用的案例。


* 本仓库是 Agileboot 快速开发脚手架的配套后端项目。后端是基于优秀的开源项目[AgileBoot-Back-End](https://github.com/valarchie/AgileBoot-Back-End)开发而成。在此感谢 AgileBoot-Back-End 的[作者](https://github.com/valarchie)。
* 前端采用Vue3、Element Plus、TypeScript、Pinia。对应前端仓库 [AgileBoot-Front-End](https://github.com/valarchie/AgileBoot-Front-End) ，保持同步更新。
* 后端采用Spring Boot、Spring Security & Jwt、Redis & Postgresql、Mybatis Plus等。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持注解式主从数据库切换，注解式请求限流，注解式重复请求拦截。
* 支持注解式菜单权限拦截，注解式数据权限拦截。
* 支持加载动态权限菜单，实时权限控制。
* ***有大量的单元测试，集成测试覆盖确保业务逻辑正确***。

> 有任何问题或者建议，可以在 _Issues_ 中提给作者。  
> 
> 您的Issue比Star更重要
>
> 如果觉得项目对您有帮助，可以来个Star ⭐


## 💥 在线体验 💥
演示地址：
<https://agileboot-front-end.pages.dev>


## 🌴 项目背景 🌴


## ✨ 使用 ✨


### 开发环境

- JDK
- Mysql
- Redis
- Node.js

### 技术栈

| 技术              | 说明              | 版本                |
|-----------------|-----------------|-------------------|
| `springboot`    | Java项目必备框架      | 2.7               |
| `druid`         | alibaba数据库连接池   | 1.2.8             |
| `springdoc`     | 文档生成            | 3.0.0             |
| `mybatis-plus`  | 数据库框架           | 3.5.2             |
| `hutool`        | 国产工具包（简单易用）     | 5.8.40            |
| `mockito`       | 单元测试模拟          | 4.11.0            |
| `guava`         | 谷歌工具包（提供简易缓存实现） | 31.0.1-jre        |
| `junit.jupiter` | 单元测试            | 5.9.2             |
| `h2`            | 内存数据库           | 1.10.19           |
| `jackson`       | 比较安全的Json框架     | follow springboot |
| `knife4j`       | 接口文档框架          | 3.0.3             |
| `Spring Task`   | 定时任务框架（适合小型项目）  | follow springboot |


### 启动说明

#### 前置准备： 下载前后端代码

```
git clone https://github.com/bruceblink/AgileBoot-Back-End
git clone https://github.com/bruceblink/AgileBoot-Front-End
```

#### 安装好Mysql和Redis

已经支持docker一键启动mysql和redis的docker实例，无需本机下载安装。使用方式：

- 安装[docker desktop](https://www.docker.com/products/docker-desktop/)(可选)

- 在linux服务器或者直接运行[docker/run.md](docker/run.md)(如果已经安装了docker desktop)中的如下命令：

  ```bash
  cd docker
  docker-compose -f docker-compose.yml -p agile-boot up -d
  ```
  
  运行无报错则成功则说明mysql和redis的服务启动成功，无需再重新导入执行sql脚本初始化数据库
  
- 关于MySQL的配置见[my.cnf](docker/mysql/conf/my.cnf)

- 在docker-compose.yml中配置了`docker-entrypoint-initdb.d`使用`sql/mysql8/*.sql`中的sql脚本进行数据库的初始化，初始化执行脚本的顺序按照脚本"名称的标号顺序"执行，原理见[docker-entrypoint-initdb.d初始化脚本的执行顺序](https://blog.likanug.top/article/20e5c571-bb7d-80d3-9f6e-c511b9923f93)，后续开发过程中只需要维护sql/mysql8目录下的sql脚本即可

- 关于redis的配置见[redis.conf](docker/redis/config/redis.conf)

#### 后端启动

```
(如果使用docker-compose搭建的开发环境，则下面的1、2两个步骤可以省略，直接从3开始)
1. 生成所需的数据库表
找到后端项目根目录下的sql目录中的agileboot_xxxxx.sql脚本文件(取最新的sql文件)。 导入到你新建的数据库中。

2. 在admin模块底下，找到resource目录下的application-dev.yml文件
配置数据库以及Redis的 地址、端口、账号密码

3. 在根目录执行mvn install

4. 找到agileboot-admin模块中的AgileBootAdminApplication启动类，直接启动即可

5. 当出现以下字样即为启动成功
  ____   _                _                                                           __         _  _ 
 / ___| | |_  __ _  _ __ | |_   _   _  _ __    ___  _   _   ___  ___  ___  ___  ___  / _| _   _ | || |
 \___ \ | __|/ _` || '__|| __| | | | || '_ \  / __|| | | | / __|/ __|/ _ \/ __|/ __|| |_ | | | || || |
  ___) || |_| (_| || |   | |_  | |_| || |_) | \__ \| |_| || (__| (__|  __/\__ \\__ \|  _|| |_| || ||_|
 |____/  \__|\__,_||_|    \__|  \__,_|| .__/  |___/ \__,_| \___|\___|\___||___/|___/|_|   \__,_||_|(_)
                                      |_|                             

```

#### 前端启动
详细步骤请查看对应前端部分

```
1. pnpm install

2. pnpm run dev

3. 当出现以下字样时即为启动成功

vite v2.6.14 dev server running at:

> Local: http://127.0.0.1:80/

ready in 4376ms.

```

详细过程在这个文章中：[AgileBoot - 手把手一步一步带你Run起全栈项目(SpringBoot+Vue3)](https://juejin.cn/post/7153812187834744845)


> 对于想要尝试全栈项目的前端人员，这边提供更简便的后端启动方式，无需配置Mysql和Redis直接启动
#### 无Mysql/Redis 后端启动
```
1. 找到agilboot-admin模块下的resource文件中的application.yml文件

2. 配置以下两个值
spring.profiles.active: basic,dev
改为
spring.profiles.active: basic,test

agileboot.embedded.mysql: false
agileboot.embedded.redis: false
改为
agileboot.embedded.mysql: true
agileboot.embedded.redis: true

请注意:高版本的MacOS系统，无法启动内置的Redis


3. 找到agileboot-admin模块中的AgileBootAdminApplication启动类，直接启动即可
```


## 🙊 系统内置功能 🙊  
  

🙂 大部分功能，均有通过 **单元测试** **集成测试** 保证质量。

|     | 功能    | 描述                              |
|-----|-------|---------------------------------|
|     | 用户管理  | 用户是系统操作者，该功能主要完成系统用户配置          |
| ⭐   | 部门管理  | 配置系统组织机构（公司、部门、小组），树结构展现支持数据权限  |
| ⭐   | 岗位管理  | 配置系统用户所属担任职务                    |
|     | 菜单管理  | 配置系统菜单、操作权限、按钮权限标识等，本地缓存提供性能    |
| ⭐   | 角色管理  | 角色菜单权限分配、设置角色按机构进行数据范围权限划分      |
|     | 参数管理  | 对系统动态配置常用参数                     |
|     | 通知公告  | 系统通知公告信息发布维护                    |
| 🚀  | 操作日志  | 系统正常操作日志记录和查询；系统异常信息日志记录和查询     |
|     | 登录日志  | 系统登录日志记录查询包含登录异常                |
|     | 在线用户  | 当前系统中活跃用户状态监控                   |
|     | 系统接口  | 根据业务代码自动生成相关的api接口文档            |
|     | 服务监控  | 监视当前系统CPU、内存、磁盘、堆栈等相关信息         |
|     | 缓存监控  | 对系统的缓存信息查询，命令统计等                |
|     | 连接池监视 | 监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈 |


## 🐯 工程结构 🐯

``` 
agileboot
├── agileboot-admin -- 管理后台接口模块（供后台调用）
│
├── agileboot-api -- 开放接口模块（供客户端调用）
│
├── agileboot-common -- 精简基础工具模块
│
├── agileboot-infrastructure -- 基础设施模块（主要是配置和集成，不包含业务逻辑）
│
├── agileboot-domain -- 业务模块
├    ├── user -- 用户模块（举例）
├         ├── command -- 命令参数接收模型（命令）
├         ├── dto -- 返回数据类
├         ├── db -- DB操作类
├              ├── entity -- 实体类
├              ├── service -- DB Service
├              ├── mapper -- DB Dao
├         ├── model -- 领域模型类
├         ├── query -- 查询参数模型（查询）
│         ├────── UserApplicationService -- 应用服务（事务层，操作领域模型类完成业务逻辑）

```

### 代码流转

请求分为两类：一类是查询，一类是操作（即对数据有进行更新）。

**查询**：Controller > xxxQuery > xxxApplicationService > xxxService(Db) > xxxMapper  
**操作**：Controller > xxxCommand > xxxApplicationService > xxxModel(处理逻辑) > save 或者 update (本项目直接采用JPA的方式进行插入已经更新数据)

这是借鉴CQRS的开发理念，将查询和操作分开处理。操作类的业务实现借鉴了DDD战术设计的理念，使用领域类，工厂类更面向对象的实现逻辑。 
如果你不太适应这样的开发模式的话。可以在domain模块中按照你之前从Controller->Service->DAO的模式进行开发。it is up to you.



### 二次开发指南

假设你要新增一个会员member业务，可以在以下三个模块新增对应的包来实现你的业务
``` 
agileboot
├── agileboot-admin -- 
│                ├── member -- 会员模块
│
├── agileboot-domain -- 
├                ├── member -- 会员模块（举例）
├                     ├── command -- 命令参数接收模型（命令）
├                     ├── dto -- 返回数据类
├                     ├── db -- DB操作类
├                          ├── entity -- 实体类
├                          ├── service -- DB Service
├                          ├── mapper -- DB Dao
├                     ├── model -- 领域模型类
├                     ├── query -- 查询参数模型（查询）
│                     ├────── MemberApplicationService -- 应用服务（事务层，操作领域模型类完成业务逻辑）
└─
```

## 🌻 注意事项 🌻
- IDEA会自动将.properties文件的编码设置为ISO-8859-1,请在Settings > Editor > File Encodings > Properties Files > 设置为UTF-8
- 请导入统一的代码格式化模板（Google）: Settings > Editor > Code Style > Java > 设置按钮 > import schema > 选择项目根目录下的GoogleStyle.xml文件
- 如需要生成新的表，请使用CodeGenerator类进行生成。
  - 填入数据库地址，账号密码，库名。然后填入所需的表名执行代码即可。（大概看一下代码就知道怎么填啦）
  - 生成的类在infrastructure模块下的target/classes目录下
  - 不同的数据库keywordsHandler方法请填入对应不同数据库handler。（搜索keywordsHandler关键字）
- 项目基础环境搭建，请参考docker目录下的指南搭建。保姆级启动说明：
  - [AgileBoot - 手把手一步一步带你Run起全栈项目(SpringBoot+Vue3)](https://juejin.cn/post/7153812187834744845)
- 注意：管理后台的后端启动类是AgileBoot**Admin**Application
- Swagger的API地址为 http://localhost:8080/v3/api-docs

## Contributors

<a href="https://github.com/bruceblink/AgileBoot-Back-End/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=bruceblink/AgileBoot-Back-End"  alt="bruceblink/AgileBoot-Back-End"/>
</a>


[![Sponsor](https://img.shields.io/badge/sponsor-30363D?style=for-the-badge&logo=GitHub-Sponsors&logoColor=#EA4AAA)](https://github.com/sponsors/bruceblink) [![Buy Me Coffee](https://img.shields.io/badge/Buy%20Me%20Coffee-FF5A5F?style=for-the-badge&logo=coffee&logoColor=FFFFFF)](https://buymeacoffee.com/bruceblink)

