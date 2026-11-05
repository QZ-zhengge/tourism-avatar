# 文旅数字人游客 API

为游客客户端提供账号、数字人、语音、天气、动态和打卡相关接口。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | FastAPI 入口 |
| [routers/avatar.py](routers/avatar.py) | 数字人交互路由 |
| [routers/checkin.py](routers/checkin.py) | 打卡接口 |
| [models/avatar_chat.py](models/avatar_chat.py) | 会话数据 |
| [utils/xfyun_auth.py](utils/xfyun_auth.py) | 外部服务鉴权 |
| [test](test) | 接口测试 |

## 一条业务链路

路由接收客户端参数，由 schema 定义边界，通过数据库和服务适配模块完成请求。展示时挑选数字人会话，从 API 追到记录模型与签名工具。

## 运行与验证

安装 requirements.txt，参照 .env.example 配置自有数据库和测试服务，再按入口启动。先用本地库检查接口，归档未复验线上环境。

## 面试可能会问

**游客 API 和管理 API 如何协作？**

二者分别为不同角色提供接口，通过约定数据模型和共享数据库协作，访问入口和权限各自控制。

**外部数字人签名放哪里？**

放在服务端工具或代理层。阅读签名模块后可以解释时间戳、待签名数据与配置来源。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
