# Anki APKG 格式详解

## 概述

`.apkg` (Anki Package) 是 Anki 使用的一种**压缩包格式**，本质上是一个 **ZIP 文件**，只是扩展名改为了 `.apkg`。这种格式用于打包和分发 Anki 牌组（deck），包含卡片数据、媒体文件和配置信息。

## 版本兼容性

### 格式版本演进

| 格式版本 | 文件名称 | 架构版本 | 支持版本 | 特性 |
|----------|----------|----------|----------|------|
| Legacy1 | collection.anki2 | V11 | Anki < 2.1.0 | 原始格式 |
| Legacy2 | collection.anki21 | V11-V18 | Anki 2.1.x | v2调度器 |
| Latest | collection.anki21b | V18 | Anki 23.10+ | 新格式，Zstd压缩、Protobuf媒体映射、meta文件 |

## 文件结构

一个典型的 `.apkg` 文件包含以下内容：

```
example.apkg (ZIP 压缩包)
├── collection.anki2 / collection.anki21 / collection.anki21b  # SQLite 数据库文件（V18为Zstd压缩）
├── media                    # 媒体映射文件（见下文，格式随版本变化）
├── meta                     # 新格式（V18）必需，Protobuf编码包版本
├── 0                        # 媒体文件（按数字编号，V18为Zstd压缩）
├── 1                        # 媒体文件
├── 2                        # 媒体文件
└── ...                      # 更多媒体文件
```

### 1. 数据库文件

根据 Anki 版本不同，数据库文件名称和架构版本有所区别：

- **collection.anki2**: 旧格式 (Schema V11)，兼容 Anki 2.1.x
- **collection.anki21**: 过渡格式 (Schema V11-V18)，v2调度器
- **collection.anki21b**: 新格式 (Schema V18)，Anki 23.10+，**内容需用 Zstd 压缩**，不是明文 SQLite

#### 主要数据表

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `col` | 集合配置 | conf, models, decks, dconf |
| `notes` | 笔记数据 | id, guid, mid, flds, tags |
| `cards` | 卡片数据 | id, nid, did, ord, due, ivl, factor |
| `revlog` | 复习历史 | id, cid, ease, ivl, time, type |
| `graves` | 删除记录 | usn, oid, type |

#### 表结构详解

**col 表 (Collection)**
```sql
CREATE TABLE col (
    id INTEGER PRIMARY KEY,      -- 集合 ID
    crt INTEGER NOT NULL,        -- 创建时间
    mod INTEGER NOT NULL,        -- 修改时间
    scm INTEGER NOT NULL,        -- 架构修改时间
    ver INTEGER NOT NULL,        -- 版本号
    dty INTEGER NOT NULL,        -- 脏标记
    usn INTEGER NOT NULL,        -- 更新序列号
    ls INTEGER NOT NULL,         -- 最后同步时间
    conf TEXT NOT NULL,          -- 配置 JSON
    models TEXT NOT NULL,        -- 模型 JSON
    decks TEXT NOT NULL,         -- 牌组 JSON
    dconf TEXT NOT NULL,         -- 牌组配置 JSON
    tags TEXT NOT NULL           -- 标签 JSON
);
```

**notes 表 (Notes)**
```sql
CREATE TABLE notes (
    id INTEGER PRIMARY KEY,      -- 笔记 ID
    guid TEXT NOT NULL,          -- 全局唯一标识符
    mid INTEGER NOT NULL,        -- 模型 ID
    mod INTEGER NOT NULL,        -- 修改时间
    usn INTEGER NOT NULL,        -- 更新序列号
    tags TEXT NOT NULL,          -- 标签
    flds TEXT NOT NULL,          -- 字段内容（用 \x1f 分隔）
    sfld TEXT NOT NULL,          -- 排序字段
    csum INTEGER NOT NULL,       -- 校验和
    flags INTEGER NOT NULL,      -- 标志位
    data TEXT NOT NULL           -- 附加数据
);
```

**cards 表 (Cards)**
```sql
CREATE TABLE cards (
    id INTEGER PRIMARY KEY,      -- 卡片 ID
    nid INTEGER NOT NULL,        -- 笔记 ID
    did INTEGER NOT NULL,        -- 牌组 ID
    ord INTEGER NOT NULL,        -- 模板序号
    mod INTEGER NOT NULL,        -- 修改时间
    usn INTEGER NOT NULL,        -- 更新序列号
    type INTEGER NOT NULL,       -- 卡片类型 (0=new, 1=learning, 2=review)
    queue INTEGER NOT NULL,      -- 队列类型
    due INTEGER NOT NULL,        -- 到期时间
    ivl INTEGER NOT NULL,        -- 间隔时间（天）
    factor INTEGER NOT NULL,     -- 容易度因子
    reps INTEGER NOT NULL,       -- 复习次数
    lapses INTEGER NOT NULL,     -- 遗忘次数
    left INTEGER NOT NULL,       -- 剩余步骤
    odue INTEGER NOT NULL,       -- 原始到期时间
    odid INTEGER NOT NULL,       -- 原始牌组 ID
    flags INTEGER NOT NULL,      -- 标志位
    data TEXT NOT NULL           -- 附加数据（FSRS 参数等）
);
```

### 2. media 文件

#### 旧格式（collection.anki2/anki21）
这是一个 JSON 文件，映射媒体文件的编号到实际文件名：

```json
{
    "0": "hello.mp3",
    "1": "world.jpg",
    "2": "example.wav"
}
```

#### 新格式（collection.anki21b，V18）
- `media` 文件内容为 Protobuf 编码的 MediaEntries（entries: repeated MediaEntry），整体经过 Zstd 压缩。
- 每个编号媒体文件（如 0、1、2）内容也需单独用 Zstd 压缩。
- Protobuf 结构（伪代码）：

```protobuf
message MediaEntries {
  repeated MediaEntry entries = 1;
}
message MediaEntry {
  string name = 1;
  uint32 size = 2;
  bytes sha1 = 3;
}
```

- 旧格式（collection.anki2/anki21）：`media` 为 JSON，编号媒体文件为原始内容。
- 新格式（collection.anki21b）：`media` 为 Zstd+Protobuf，编号媒体文件为 Zstd 压缩内容。

### 3. meta 文件（仅新格式 V18 必需）

- 新格式（V18）APKG 必须包含 `meta` 文件，内容为 Protobuf 编码，字段 1（varint）表示包格式版本。
- 示例（仅含 version 字段）：

```protobuf
message Meta {
  uint32 version = 1; // 1=LEGACY_1, 2=LEGACY_2, 3=LATEST
}
```

## JSON 配置格式

### models (模型配置)

```json
{
    "1234567890": {
        "id": 1234567890,
        "name": "Basic",
        "type": 0,
        "mod": 1632150000,
        "usn": -1,
        "sortf": 0,
        "did": null,
        "tmpls": [
            {
                "name": "Card 1",
                "ord": 0,
                "qfmt": "{{Front}}",
                "afmt": "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}",
                "did": null,
                "bqfmt": "",
                "bafmt": ""
            }
        ],
        "flds": [
            {
                "name": "Front",
                "ord": 0,
                "sticky": false,
                "rtl": false,
                "font": "Arial",
                "size": 20
            },
            {
                "name": "Back",
                "ord": 1,
                "sticky": false,
                "rtl": false,
                "font": "Arial",
                "size": 20
            }
        ],
        "css": ".card {\n font-family: arial;\n font-size: 20px;\n text-align: center;\n color: black;\n background-color: white;\n}"
    }
}
```

### decks (牌组配置)

```json
{
    "1": {
        "id": 1,
        "mod": 0,
        "name": "Default",
        "usn": 0,
        "lrnToday": [0, 0],
        "revToday": [0, 0],
        "newToday": [0, 0],
        "timeToday": [0, 0],
        "collapsed": true,
        "browserCollapsed": true,
        "desc": "",
        "dyn": 0,
        "conf": 1,
        "extendNew": 0,
        "extendRev": 0,
        "reviewLimit": null,
        "newLimit": null,
        "reviewLimitToday": null,
        "newLimitToday": null
    }
}
```

### dconf (牌组学习配置)

```json
{
    "1": {
        "name": "Default",
        "replayq": true,
        "lapse": {
            "delays": [10],
            "mult": 0,
            "minInt": 1,
            "leechFails": 8,
            "leechAction": 0
        },
        "rev": {
            "perDay": 200,
            "ease4": 1.3,
            "fuzz": 0.05,
            "minSpace": 1,
            "ivlFct": 1,
            "maxIvl": 36500,
            "ease2": 1.2,
            "bury": true,
            "hardFactor": 1.2
        },
        "timer": 0,
        "maxTaken": 60,
        "usn": 0,
        "new": {
            "delays": [1, 10],
            "ints": [1, 4, 7],
            "initialFactor": 2500,
            "separate": true,
            "bury": true,
            "perDay": 20,
            "order": 1
        },
        "mod": 0,
        "id": 1,
        "autoplay": true
    }
}
```

## FSRS 支持

对于支持 FSRS (Free Spaced Repetition Scheduler) 的 Anki 版本，卡片的 `data` 字段可能包含 FSRS 相关参数：

```json
{
    "s": 2.5,
    "d": 1.2,
    "r": 0.8
}
```

## 创建 APKG 的步骤

1. **创建 SQLite 数据库**
   - 创建必要的表结构
   - 插入集合配置数据
   - 插入笔记和卡片数据

2. **准备媒体文件**
   - 收集所有引用的媒体文件
   - 创建编号映射关系
   - 生成 media JSON 文件

3. **打包为 ZIP**
   - 将数据库文件添加为 `collection.anki2`
   - 将媒体映射添加为 `media`
   - 将媒体文件按编号添加
   - 保存为 `.apkg` 扩展名

## 字段分隔符

在 `notes.flds` 中，多个字段使用 ASCII 字符 `\x1f` (Unit Separator) 分隔。

例如：`"Hello\x1f你好\x1f[sound:hello.mp3]\x1fHello, world!"`

## 兼容性说明

### Anki 23.10+ 重大变更
- **格式破坏性变更**: 新格式 (`collection.anki21b`) 与旧版本不兼容
- **Zstd 压缩**: 默认启用压缩以获得更小文件大小，collection.anki21b、media、所有媒体文件内容均需 Zstd 压缩
- **媒体处理**: 媒体列表格式从哈希映射（JSON）改为 Protobuf+Zstd 结构
- **meta 文件**: 新格式包内必须包含 meta 文件，内容为 Protobuf 编码的包版本
- **内置特性**: 支持 FSRS 集成调度和图像遮挡

### 版本特定要求
- **Anki 24.11+**: 使用最新的 decks JSON 结构（包含 `mod`, `browserCollapsed`, `dyn`, `reviewLimit` 等字段）
- **数据库版本**: 根据目标版本选择 V11 (兼容旧版) 或 V18 (新版)
- **字段顺序**: 确保字段顺序与 Anki 官方结构完全一致
- **媒体文件名规范**: 新格式下需做安全化处理（去除非法字符、避免 Windows 保留名、去除路径穿越等），并在重名时加 hash 后缀

### 全面兼容性策略
建议同时支持两种格式生成：
1. **新格式** (`collection.anki21b`): 针对 Anki 23.10+ 用户，所有媒体相关内容和数据库均需 Zstd 压缩，media 文件为 Protobuf，不再是 JSON。
2. **旧格式** (`collection.anki2`/`collection.anki21`): 针对 Anki 2.1.x - 23.09 用户，media 为 JSON，媒体文件为原始内容。

- 推荐生成双格式 APKG，确保兼容所有主流 Anki 版本。
- 字段顺序和 JSON 结构需严格对齐 Anki 官方格式。

## 注意事项

1. **时间戳**：所有时间字段使用 Unix 时间戳（秒）
2. **ID 生成**：使用毫秒时间戳确保唯一性
3. **字符编码**：所有文本使用 UTF-8 编码
4. **媒体引用**：在字段中使用 `[sound:filename.mp3]` 或 `<img src="filename.jpg">` 格式
5. **兼容性**：确保生成的文件与目标 Anki 版本兼容

## 相关工具

本项目提供了 `ApkgCreator.kt` 类用于创建 APKG 文件：

- 支持创建基础和高级卡片模板
- 自动处理媒体文件映射
- 兼容 FSRS 算法参数
- 提供简单易用的 API

## 参考资源

- [Anki 官方文档](https://docs.ankiweb.net/)
- [Anki 数据库格式](https://github.com/ankidroid/Anki-Android/wiki/Database-Structure)
- [FSRS 算法](https://github.com/open-spaced-repetition/fsrs4anki)
- [Anki GitHub 仓库](https://github.com/ankitects/anki) - 最新格式变更信息
