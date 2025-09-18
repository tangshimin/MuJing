# APKG 测试文件格式说明

## 格式类型覆盖

### 1. 过渡格式 (Anki 2.1.x)
**basic_word.apkg** - 初中英语词汇学习  
- **更新时间**: 2025-03-14
- **格式**: `collection.anki21` (过渡格式)
- **架构版本**: 46
- **数据库版本**: 11
- **文件大小**: 356KB
- **特点**: Anki 2.1.x 系列的标准导出格式

### 2. 新格式 (Anki 23.10+)
**basic_word_new_version.apkg** - 初中英语词汇学习牌组使用新版 Anki 重新导出的版本
- **更新时间**: 2025-09-17  
- **格式**: `collection.anki21b` (新格式) + `collection.anki2` (兼容版本)
- **架构版本**: 47
- **数据库版本**: 11
- **文件大小**: 71KB (Zstd压缩)
- **特点**: Anki 23.10+ 新格式，Zstd压缩，包含双向兼容

### 3. 原始旧格式 (Anki 2.1之前)
**primary_school_words.apkg** - 苏教版小学英语单词表
- **更新时间**: 2017-01-17
- **格式**: `collection.anki2` (原始格式)
- **架构版本**: 14
- **数据库版本**: 11
- **文件大小**: 295KB
- **特点**: 最旧的 Anki 格式，兼容性最好

### 4. 新格式示例
**Alphabet Word.apkg** - 字母单词学习
- **更新时间**: 2025-09-17
- **格式**: `collection.anki21b` (新格式) + `collection.anki2` (兼容版本)
- **架构版本**: 47
- **数据库版本**: 11
- **文件大小**: 7.5KB (Zstd压缩)
- **特点**: 小型新格式示例，展示压缩效果

## 格式演进总结

| 文件 | 格式类型 | Anki版本 | 压缩 | 大小 | 特点 |
|------|----------|----------|------|------|------|
| primary_school_words.apkg | collection.anki2 | <2.1.0 | 无 | 295KB | 原始格式 |
| basic_word.apkg | collection.anki21 | 2.1.x | 无 | 356KB | 过渡格式 |
| basic_word_new_version.apkg | collection.anki21b | 23.10+ | Zstd | 71KB | 新格式 |
| Alphabet Word.apkg | collection.anki21b | 23.10+ | Zstd | 7.5KB | 小型新格式 |

这些文件提供了完整的 APKG 格式覆盖，用于测试不同版本的兼容性。
