# 文旅游客 API 测试快照

记录游客端在测试集成目录中的历史接口和测试。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | 历史应用入口 |
| [routers/avatar.py](routers/avatar.py) | 数字人接口 |
| [routers/post.py](routers/post.py) | 动态业务 |
| [database/database.py](database/database.py) | 数据库入口 |
| [test](test) | 历史测试 |

## 一条业务链路

从路由及测试对照当前游客 API 的数据与错误约定。适合用来讲同一服务不同工作树的演进。

## 运行与验证

安装 requirements.txt，用示例环境变量和独立测试数据库。联调和部署以游客 API 主目录为准。

## 面试可能会问

**为什么同提交还要留两个快照？**

提交相同并不代表工作树相同。保留差异才能复盘当时的集成修改。

**最值得比对哪类测试？**

同一 API 的正常、鉴权失败和边界参数用例。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
