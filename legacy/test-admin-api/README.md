# 文旅管理 API 测试快照

保留后台服务在测试集成目录中的配置、接口及模型。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | 应用入口 |
| [routers](routers) | 后台接口 |
| [models/admin.py](models/admin.py) | 管理数据模型 |
| [API文档.md](API%E6%96%87%E6%A1%A3.md) | 历史接口描述 |

## 一条业务链路

从数字人、对话或知识库路由定位历史请求格式，跟到模型后与当前版比较。

## 运行与验证

准备本地 Python 依赖、测试库及配置模板。面向当前后端的展示从 admin-api 主目录开始。

## 面试可能会问

**如何校验接口文档和实现一致？**

从路由签名与响应模型提取接口，再用客户端契约测试覆盖主要字段。

**为什么不用历史快照直接上线？**

应先确认数据迁移、权限和当前前端契约，并用独立环境验证。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
