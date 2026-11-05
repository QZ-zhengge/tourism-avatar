# 用户建议模块 — 后端 API 接口规范

## 数据库表：`suggestions`

```sql
CREATE TABLE `suggestions` (
  `id`          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '用户标识（游客端open_id或设备ID）',
  `user_name`   VARCHAR(100) NOT NULL DEFAULT '匿名用户' COMMENT '用户昵称',
  `content`     TEXT         NOT NULL COMMENT '建议内容',
  `status`      ENUM('unread','read','archived') NOT NULL DEFAULT 'unread' COMMENT '状态: 未读/已读/已归档',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客用户建议表';
```

---

## API 接口

### 1. 获取建议列表

```
GET /api/v1/admin/suggestions
```

**Query 参数**（可选）:
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 筛选状态：`unread` / `read` / `archived`，不传返回全部 |
| limit | int | 每页数量，默认 50 |
| offset | int | 偏移量，默认 0 |

**响应示例**:
```json
{
  "items": [
    {
      "id": 1,
      "user_id": "openid_abc123",
      "user_name": "游客 A",
      "content": "希望能增加景区地图导航功能",
      "status": "unread",
      "created_at": "2026-06-14T10:30:00Z"
    },
    {
      "id": 2,
      "user_id": "openid_def456",
      "user_name": "匿名用户",
      "content": "今天的表演时间改了没有通知，建议加个公告栏",
      "status": "read",
      "created_at": "2026-06-13T15:20:00Z"
    }
  ],
  "total": 2
}
```

### 2. 标记为已读

```
PUT /api/v1/admin/suggestions/{id}/read
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 建议 ID |

**响应**: `200 OK`

### 3. 标记为已归档

```
PUT /api/v1/admin/suggestions/{id}/archive
```

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 建议 ID |

**响应**: `200 OK`

### 4. 游客端提交建议（供游客端开发使用）

```
POST /api/v1/guide/suggestions
```

**Body** (JSON):
```json
{
  "user_id": "openid_abc123",
  "user_name": "游客 A",
  "content": "希望能增加景区地图导航功能"
}
```

**响应**:
```json
{
  "id": 3,
  "message": "感谢您的建议，我们会尽快处理！"
}
```

---

## 前端行为说明

- 点击右上角信箱图标 → 打开面板 → 自动请求 `GET /api/v1/admin/suggestions` 加载数据
- 每条未读建议（`status: "unread"`）左侧有蓝色圆点标识，面板顶部徽标显示未读数
- 点击某条建议 → 前端立即标记为已读（更新状态 + 减少未读计数）→ 再异步调用 `PUT /api/v1/admin/suggestions/{id}/read`
- 如果接口暂时不可用，前端会静默降级，不影响其他功能
