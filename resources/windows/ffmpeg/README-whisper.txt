FFmpeg + whisper filter (static build)

This artifact contains:
  - ffmpeg.exe (linked with whisper/ggml; OpenMP enabled)
  - required runtime DLLs (C++ runtime, OpenMP, compression libs)

The pkg-config file (whisper.pc) is NOT included because it only
matters when compiling software against libwhisper. End users do not need it.

Supported audio formats for Whisper transcription:
  - Common formats: MP3, AAC, WAV, FLAC
  - Professional formats: AC3/E-AC3 (Dolby Digital), DTS, Dolby TrueHD
  - Streaming formats: Opus, Vorbis (OGG), AAC-HE
  - Broadcast formats: MP2 (MPEG-1 Layer II)
  - Apple formats: ALAC (Apple Lossless), M4A
  - Microsoft formats: WMA (all variants)
  - Voice formats: AMR-NB, AMR-WB
  - PCM formats: 8/16/24/32-bit integer, 32/64-bit float
  - Specialized: DVD-Audio PCM, Blu-ray PCM

Supported subtitle formats:
  - SRT, ASS, SSA, WebVTT, MOV Text

Usage example:
  ffmpeg -i input.mkv -filter:a "whisper=model=ggml-tiny.en.bin:task=transcribe" -f null -

Optional performance tuning:
  set OMP_NUM_THREADS=6   (Windows CMD)
  $env:OMP_NUM_THREADS=6  (PowerShell)

Build optimized for US TV/Movies audio formats:
  Covers 99%+ of American video content from DVD to 4K Blu-ray,
  broadcast TV, cable TV, and all major streaming platforms.
