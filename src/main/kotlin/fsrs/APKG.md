# Anki APKG 格式详解

## 概述

`.apkg` (Anki Package) 是 Anki 使用的一种**压缩包格式**，本质上是一个 **ZIP 文件**，只是扩展名改为了 `.apkg`。这种格式用于打包和分发 Anki 牌组（deck），包含卡片数据、媒体文件和配置信息。

## 文件结构

一个典型的 `.apkg` 文件包含以下内容：

```
example.apkg (ZIP 压缩包)
├── collection.anki2          # SQLite 数据库文件（核心数据）
├── media                     # JSON 文件（媒体文件映射）
├── 0                        # 媒体文件（按数字编号）
├── 1                        # 媒体文件
├── 2                        # 媒体文件
└── ...                      # 更多媒体文件
```

### 1. collection.anki2

这是一个 **SQLite 数据库文件**，包含所有的卡片数据和配置信息。

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

这是一个 JSON 文件，映射媒体文件的编号到实际文件名：

```json
{
    "0": "hello.mp3",
    "1": "world.jpg",
    "2": "example.wav"
}
```

### 3. 媒体文件

按数字编号的实际媒体文件（图片、音频、视频等），对应 media 文件中的映射关系。

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
    "s": 2.5,      // Stability (稳定性)
    "d": 1.2,      // Difficulty (难度)
    "r": 0.8       // Retrievability (可提取性)
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

- **Anki 24.11+**: 使用最新的 decks JSON 结构（包含 `mod`, `browserCollapsed`, `dyn`, `reviewLimit` 等字段）
- **数据库版本**: 设置为 11 以确保与 Anki 2.1.x 版本兼容
- **字段顺序**: 确保字段顺序与 Anki 官方结构完全一致

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
