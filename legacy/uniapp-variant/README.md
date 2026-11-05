# 随行 UniApp 历史工作副本

保留跨端实现的另一工作版本，适合比较页面与数字人交互变化。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [App.vue](App.vue) | 应用入口 |
| [pages.json](pages.json) | 页面定义 |
| [pages/digital-stream](pages/digital-stream) | 数字人页面 |
| [config/index.js](config/index.js) | 环境配置 |
| [utils/xunfei-ws.js](utils/xunfei-ws.js) | 连接逻辑 |

## 一条业务链路

按页面注册追踪数字人界面和连接模块，再与主版配置、页面和接口封装做差异比较。

## 运行与验证

用 HBuilderX 和自有测试服务复现。作为历史快照，开发主线使用旅游项目中的 uniapp 目录。

## 面试可能会问

**为什么单独归档？**

副本保留与主版不同的页面和配置，可查找曾经尝试的实现。

**怎样判断差异有价值？**

先排除构建和配置噪声，再比较页面行为、数据契约和平台兼容性。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
