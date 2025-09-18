# APKG ZSTD 兼容性问题分析

## 问题描述
ApkgCreator 生成的 APKG 文件（新格式 `collection.anki21b`）在导入到 Anki 24.11+ 时失败，错误信息："500: Unknown frame descriptor"

## 根本原因分析

### 1. Zstd 版本不匹配

**Anki 24.11+ 使用的 Zstd 版本：**
- Rust zstd 库：0.13.3
- zstd-safe：7.2.4
- 压缩级别：0（无压缩）

**ApkgCreator 使用的 Zstd 版本：**
- Java zstd-jni 库：1.5.6-10
- 压缩级别：1（轻微压缩）

### 2. 压缩参数差异

**Anki 压缩方式：**
```rust
fn zstd_copy(reader: &mut impl Read, writer: &mut impl Write, size: usize) -> Result<()> {
    let mut encoder = Encoder::new(writer, 0)?;  // 压缩级别 0
    if size > MULTITHREAD_MIN_BYTES {
        encoder.multithread(num_cpus::get() as u32)?;
    }
    io::copy(reader, &mut encoder)?;
    encoder.finish()?;
    Ok(())
}
```

**ApkgCreator 压缩方式：**
```kotlin
private fun compressWithZstdJni(data: ByteArray): ByteArray {
    val compressedData = com.github.luben.zstd.Zstd.compress(data, 1)  // 压缩级别 1
    // ...
}
```

### 3. 关键差异
1. **压缩级别不同**：Anki 使用级别 0（无压缩），ApkgCreator 使用级别 1
2. **库版本不同**：不同版本的 zstd 库可能产生不兼容的帧格式
3. **多线程处理**：Anki 对大文件启用多线程压缩

## 错误分析

"Unknown frame descriptor" 错误通常表示：
1. Zstd 帧头格式不兼容
2. 使用了新版本 zstd 的特性，而旧版本无法识别
3. 压缩参数不匹配导致帧格式异常

## 解决方案

### 短期修复
1. **使用压缩级别 0**：与 Anki 保持一致
2. **禁用压缩**：对于兼容性，可以考虑暂时禁用 Zstd 压缩

### 长期解决方案
1. **版本对齐**：确保使用与 Anki 相同版本的 zstd 库
2. **参数一致性**：完全复制 Anki 的压缩参数
3. **兼容性测试**：建立完整的兼容性测试件

## 代码修改建议

```kotlin
private fun compressWithZstdJni(data: ByteArray): ByteArray {
    try {
        // 使用与 Anki 相同的压缩级别 0
        val compressedData = com.github.luben.zstd.Zstd.compress(data, 0)
        
        // 验证压缩结果
        if (compressedData.isEmpty()) {
            throw RuntimeException("Zstd compression failed: empty result")
        }
        
        // 验证 Zstd 魔术字节
        if (compressedData.size >= 4) {
            val magic = (compressedData[0].toLong() and 0xFF) shl 24 or
                       ((compressedData[1].toLong() and 0xFF) shl 16) or
                       ((compressedData[2].toLong() and 0xFF) shl 8) or
                       (compressedData[3].toLong() and 0xFF)
            
            if (magic != 0x28B52FFDL) {
                throw RuntimeException("Invalid Zstd magic bytes: 0x${magic.toString(16)}")
            }
        }
        
        return compressedData
    } catch (e: Exception) {
        throw RuntimeException("Zstd compression failed: ${e.message}", e)
    }
}
```

## 测试验证

修复后应验证以下场景：
1. ✅ APKG 文件能够成功导入 Anki 24.11+
2. ✅ 数据库内容完整无误
3. ✅ 媒体文件正确处理
4. ✅ 双格式兼容性（旧格式 + 新格式）

## 参考资源

- [Anki 源代码](https://github.com/ankitects/anki)
- [Zstd 官方文档](https://facebook.github.io/zstd/)
- [zstd-jni GitHub](https://github.com/luben/zstd-jni)

## 时间线

- **2025-09-18**：发现并分析问题
- **2025-09-18**：实现修复方案
- **2025-09-18**：验证兼容性