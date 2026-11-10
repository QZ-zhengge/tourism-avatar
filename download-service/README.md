# 文旅应用下载服务

展示 APK 信息、提供下载并记录下载次数。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [main.py](main.py) | FastAPI 应用、SQLite 初始化和下载接口 |
| [static](static) | 静态页面资源 |
| [部署指南.md](%E9%83%A8%E7%BD%B2%E6%8C%87%E5%8D%97.md) | 部署参考 |

## 运行与验证

安装 requirements.txt，准备自有测试 APK 与本地数据路径，再按部署指南启动。真实 APK 和 SQLite 记录另行准备。
