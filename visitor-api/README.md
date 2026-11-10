# 文旅数字人游客 API

为游客客户端提供账号、数字人、语音、天气、动态和打卡相关接口。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | FastAPI 入口 |
| [routers/avatar.py](routers/avatar.py) | 数字人交互路由 |
| [routers/checkin.py](routers/checkin.py) | 打卡接口 |
| [models/avatar_chat.py](models/avatar_chat.py) | 会话数据 |
| [utils/xfyun_auth.py](utils/xfyun_auth.py) | 外部服务鉴权 |
| [test](test) | 接口测试 |

## 运行与验证

安装 requirements.txt，参照 .env.example 配置自有数据库和测试服务，再按入口启动。先用本地库检查接口，归档未复验线上环境。

## 继续阅读

- [原项目说明与操作细节](README-REFERENCE.md)
