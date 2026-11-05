# 文旅数字人 uni-app 项目

## 项目结构

每个页面均拆分为三个独立文件：

```
pages/
├── home/
│   ├── index.vue    # 模板（template）
│   ├── index.js     # 逻辑（Vue component options）
│   └── index.css    # 样式
├── moments/
│   ├── index.vue
│   ├── index.js
│   └── index.css
├── digital-human/
│   ├── index.vue
│   ├── index.js
│   └── index.css
├── map/
│   ├── index.vue
│   ├── index.js
│   └── index.css
├── scenic/
│   ├── index.vue
│   ├── index.js
│   └── index.css
└── mine/
    ├── index.vue
    ├── index.js
    └── index.css

components/
└── bottom-nav/
    ├── index.vue    # 模板
    ├── index.js     # 逻辑
    └── index.css    # 样式
```

`index.vue` 通过以下方式引入外部 JS 和 CSS：
- `import script from './index.js'` → 引入逻辑
- `@import './index.css'` → 引入样式

## 页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 首页 | /pages/home/index | 轮播图、8宫格、快捷入口、旅友圈精选 |
| 旅友圈 | /pages/moments/index | 朋友圈样式、发布、点赞、评论、删除、自定义tabbar |
| 数字人导航 | /pages/digital-human/index | 数字人播放、快捷问题、字幕 |
| 地图 | /pages/map/index | 占位页面 |
| 图谱 | /pages/scenic/index | 占位页面 |
| 我的 | /pages/mine/index | 占位页面 |
| 数字人直播（原有） | /pages/index/index | 原有数字人流媒体直播页（单文件 Vue） |

## 配置说明

### 腾讯位置服务
打开 `config/index.js`，将 `QQ_MAP_KEY` 替换为你自己的腾讯位置服务 Key。

### 后端地址
数字人相关页面连接后端地址：`http://192.0.2.1:8000`

## 静态资源占位

- `static/tabbar/*.png` — tabBar 图标（需替换）
- `static/assets/posts/*.jpg` — 旅友圈帖子图片（需替换）
- `static/assets/carousel/hero_1.jpg` — 旅友圈横幅（需替换）

## 使用方法

1. 用 HBuilderX 打开 `Z:\stuio_uniapp`
2. 替换占位图标和图片
3. 修改 `config/index.js` 中的腾讯位置服务 Key
4. 运行到浏览器或手机预览
