# 文旅数字人管理 API

为景区后台提供数字人配置、内容、知识库、对话分析和权限入口。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | 应用和路由装配 |
| [routers/digital.py](routers/digital.py) | 数字人配置 |
| [routers/dialogue.py](routers/dialogue.py) | 对话分析 |
| [routers/auth.py](routers/auth.py) | 管理员鉴权 |
| [models/admin.py](models/admin.py) | 管理端持久化模型 |

## 运行与验证

安装 requirements.txt，参照 .env.example 使用自己的测试 MySQL，并配好所需外部服务。业务库记录由测试者自行创建。

## 继续阅读

- [原项目说明与操作细节](README-REFERENCE.md)
