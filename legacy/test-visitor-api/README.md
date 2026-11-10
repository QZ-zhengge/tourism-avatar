# 文旅游客 API 测试快照

记录游客端在测试集成目录中的历史接口和测试。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | 历史应用入口 |
| [routers/avatar.py](routers/avatar.py) | 数字人接口 |
| [routers/post.py](routers/post.py) | 动态业务 |
| [database/database.py](database/database.py) | 数据库入口 |
| [test](test) | 历史测试 |

## 运行与验证

安装 requirements.txt，用示例环境变量和独立测试数据库。联调和部署以游客 API 主目录为准。

## 继续阅读

- [原项目说明与操作细节](README-REFERENCE.md)
