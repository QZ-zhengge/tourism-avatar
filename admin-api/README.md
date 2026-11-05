# 文旅数字人管理 API

为景区后台提供数字人配置、内容、知识库、对话分析和权限入口。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | 应用和路由装配 |
| [routers/digital.py](routers/digital.py) | 数字人配置 |
| [routers/dialogue.py](routers/dialogue.py) | 对话分析 |
| [routers/auth.py](routers/auth.py) | 管理员鉴权 |
| [models/admin.py](models/admin.py) | 管理端持久化模型 |

## 一条业务链路

以对话分析为例，管理前端请求统计接口，路由根据模型读取游客端产生的数据，再返回给后台。讲解时同时说明权限和数据最小化。

## 运行与验证

安装 requirements.txt，参照 .env.example 使用自己的测试 MySQL，并配好所需外部服务。业务库记录由测试者自行创建。

## 面试可能会问

**为何将管理接口独立？**

管理操作与游客操作在权限、审计和流量上不同，独立入口方便控制访问和发布。

**两个应用共享库要注意什么？**

明确表与迁移的责任，避免各端同时修改表结构，也避免统计查询影响在线业务。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
