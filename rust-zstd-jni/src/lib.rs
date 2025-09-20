use anyhow::{Context, Result};
use jni::objects::{JByteArray, JObject};
use jni::sys::{jbyteArray, jint, jstring};
use jni::JNIEnv;
use std::io::{Read, Write};

fn zstd_compress_internal(input: &[u8], level: i32) -> Result<Vec<u8>> {
    let lvl = if level == 0 { 3 } else { level };
    // 使用高层 Encoder，并设置 pledged 源大小 + 校验和 + 不写 content size
    let mut enc = zstd::stream::Encoder::new(Vec::with_capacity(input.len() / 2 + 64), lvl)
        .context("failed to create zstd encoder")?;
    let _ = enc.set_pledged_src_size(Some(input.len() as u64));
    enc.include_checksum(true)?;
    enc.include_contentsize(false)?;
    enc.write_all(input).context("zstd write_all failed")?;
    let out = enc.finish().context("zstd finish failed")?;
    Ok(out)
}

fn zstd_decompress_internal(input: &[u8]) -> Result<Vec<u8>> {
    // 先尝试一次性解码
    match zstd::stream::decode_all(&mut &*input) {
        Ok(v) => Ok(v),
        Err(e) => {
            // 回退到流式
            let mut dec = zstd::stream::Decoder::new(&*input)
                .context(format!("stream decoder creation failed: {}", e))?;
            let mut out = Vec::new();
            dec.read_to_end(&mut out)
                .context("stream decompression failed")?;
            Ok(out)
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_fsrs_zstd_ZstdNative_compress(
    mut env: JNIEnv,
    _this: JObject,
    input: JByteArray,
    compression_level: jint,
) -> jbyteArray {
    let bytes = match env.convert_byte_array(input) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("JNI: read input failed: {}", e));
            return std::ptr::null_mut();
        }
    };
    match zstd_compress_internal(&bytes, compression_level as i32) {
        Ok(out) => match env.byte_array_from_slice(&out) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new("java/lang/RuntimeException", format!("JNI: create byte array failed: {}", e));
                std::ptr::null_mut()
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Zstd compress failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_fsrs_zstd_ZstdNative_decompress(
    mut env: JNIEnv,
    _this: JObject,
    input: JByteArray,
) -> jbyteArray {
    let bytes = match env.convert_byte_array(input) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("JNI: read input failed: {}", e));
            return std::ptr::null_mut();
        }
    };
    match zstd_decompress_internal(&bytes) {
        Ok(out) => match env.byte_array_from_slice(&out) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new("java/lang/RuntimeException", format!("JNI: create byte array failed: {}", e));
                std::ptr::null_mut()
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Zstd decompress failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_fsrs_zstd_ZstdNative_compressStream(
    env: JNIEnv,
    _this: JObject,
    input: JByteArray,
    compression_level: jint,
) -> jbyteArray {
    Java_fsrs_zstd_ZstdNative_compress(env, _this, input, compression_level)
}

#[no_mangle]
pub extern "system" fn Java_fsrs_zstd_ZstdNative_getVersion(
    mut env: JNIEnv,
    _this: JObject,
) -> jstring {
    // 使用 zstd 库版本号
    let v = zstd::zstd_safe::version_number();
    let s = format!("{}.{}.{}", v / 10000, (v / 100) % 100, v % 100);
    match env.new_string(s) {
        Ok(js) => js.into_raw(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("JNI: new_string failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_compress_magic_and_fd() {
        let input = b"hello zstd apkg test data";
        let out = zstd_compress_internal(input, 0).expect("compress ok");
        assert!(out.len() >= 5, "compressed too short");
        assert_eq!(out[0], 0x28);
        assert_eq!(out[1], 0xB5);
        assert_eq!(out[2], 0x2F);
        assert_eq!(out[3], 0xFD);
        let fd = out[4];
        // 只接受 Anki 的单段+校验 FD=0x24
        assert_eq!(fd, 0x24, "unexpected frame descriptor: 0x{:02X}", fd);
    }

    #[test]
    fn test_roundtrip() {
        let input = (0..10_000u32).flat_map(|x| x.to_le_bytes()).collect::<Vec<_>>();
        let compressed = zstd_compress_internal(&input, 0).expect("compress");
        let decompressed = zstd_decompress_internal(&compressed).expect("decompress");
        assert_eq!(input, decompressed);
    }
}
