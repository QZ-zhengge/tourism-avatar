# 景区数字人管理平台 — 管理端后端

> 无锡灵山胜境 · Vue 3 前端配套后端  
> 与主项目 `SoftwareCupAI` 共享同一 MySQL 数据库 `hbuilder_app`  
> 接口文档：见同目录 `API文档.md`

---

## 快速启动

```bash
Z:\SoftwareCupAI_admin\.venv\Scripts\python.exe main.py
# Swagger: http://localhost:8005/docs
```

---

## 项目结构

```
Z:\SoftwareCupAI_admin/
├── main.py                    # FastAPI 入口，端口 8005
├── config.py                  # 共享主项目数据库配置
├── requirements.txt
├── API文档.md                 # 完整接口文档
├── database/
│   └── database.py            # SQLAlchemy + 自动建管理端 8 张表
├── models/
│   └── admin.py               # 6 个 ORM 模型
├── schemas/
│   └── admin.py               # 请求/响应 Pydantic 模型
├── routers/
│   ├── digital.py             # 数字人配置 + 形象列表（含 base64 头像）
│   ├── content.py             # 景点图片上传/列表/删除
│   ├── knowledge.py           # 知识库源文件管理
│   ├── dialogue.py            # 对话分析
│   ├── vcn.py                 # 发音人管理（含试听音频）
│   ├── auth.py                # 管理员登录/注册/JWT
│   └── suggestion.py          # 用户建议信箱
└── uploads/
    ├── avatars/               # 数字人头像（{avatar_id}.png）
    ├── vcns/                  # 发音人试听音频（{vcn}.mp3）
    ├── images/                # 景点图片
    └── documents/             # 知识库源文件
```

---

## 接口总览

| 模块 | 接口数 | 说明 |
|------|:---:|------|
| 登录注册 | 4 | 注册/登录(JWT)/获取用户信息/退出 |
| 信箱 | 2 | 用户建议列表 + 标记已读 |
| 数字人管理 | 3 | 配置读写 + 12形象列表（含 base64 头像） |
| 发音人管理 | 4 | 发音人增删 + 列表（含试听 audio_base64） |
| 内容管理 | 3 | 景点图片上传/列表/删除 |
| 知识库 | 5 | 源文件列表/详情/内容/上传/删除（支持 txt/md/docx/pdf） |
| 对话分析 | 2 | 会话列表 + 消息详情 |

全部接口前缀 `/api/v1/admin/`，详见 `API文档.md`。

**Auth/信箱**: `code=0` 表示成功（与文档对齐）。

---

## 数据库

与主项目共享 `hbuilder_app` 库，管理端独有 8 张表：

| 表名 | 说明 |
|------|------|
| `admin_users` | 管理员账号（username/email/password/role） |
| `digital_human_config` | 数字人配置（avatar_id/vcn/speed 等） |
| `vcn_voices` | 发音人元数据 |
| `scenic_images` | 景点图片 |
| `knowledge_documents` | 知识库源文件 |
| `dialogue_sessions` | 对话会话 |
| `dialogue_messages` | 对话消息 |
| `user_suggestions` | 用户建议信箱（id/content/status） |

> 主项目的 `/avatar/start` 和 `/avatar/drive/text` 每次请求实时读取 `digital_human_config` 表获取 avatar_id 和 vcn，管理端修改后自动同步到用户端。

---

## 手动配置数据

### 数字人头像

将头像图片命名为 `{avatar_id}.png` 放入 `uploads/avatars/`：

```
uploads/avatars/
├── 118801001.png   (依丹)
├── 111260002.png   (雨菲)
├── 110026010.png   (伊凡)
...
```

### 发音人试听音频

将音频文件命名为 `{vcn}.mp3` 放入 `uploads/vcns/`，或在管理端通过 `POST /vcn/voices` + `POST /vcn/voices/{vcn}/audio` 上传。
