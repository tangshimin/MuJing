8.122 whisper
It runs automatic speech recognition using the OpenAI’s Whisper model.

It requires the whisper.cpp library (https://github.com/ggml-org/whisper.cpp) as a prerequisite. After installing the library it can be enabled using: ./configure --enable-whisper.

The filter has following options:

**model**
The file path of the downloaded whisper.cpp model (mandatory).

**language**
The language to use for transcription (’auto’ for auto-detect). Default value: "auto"

**queue**
The maximum size that will be queued into the filter before processing the audio with whisper. Using a small value the audio stream will be processed more often, but the transcription quality will be lower and the required processing power will be higher. Using a large value (e.g. 10-20s) will produce more accurate results using less CPU (as using the whisper-cli tool), but the transcription latency will be higher, thus not useful to process real-time streams. Consider using the vad_model option associated with a large queue value. Default value: "3"

**use_gpu**
If the GPU support should be enabled. Default value: "true"

**gpu_device**
The GPU device index to use. Default value: "0"

**destination**
If set, the transcription output will be sent to the specified file or URL (use one of the FFmpeg AVIO protocols); otherwise, the output will be logged as info messages. The output will also be set in the "lavfi.whisper.text" frame metadata. If the destination is a file and it already exists, it will be overwritten.

**format**
The destination format string; it could be "text" (only the transcribed text will be sent to the destination), "srt" (subtitle format) or "json". Default value: "text"

**vad_model**
Path to the VAD model file. If set, the filter will load an additional voice activity detection module (https://github.com/snakers4/silero-vad) that will be used to fragment the audio queue; use this option setting a valid path obtained from the whisper.cpp repository (e.g. "../whisper.cpp/models/ggml-silero-v5.1.2.bin") and increase the queue parameter to a higher value (e.g. 20).

**vad_threshold**
The VAD threshold to use. Default value: "0.5"

**vad_min_speech_duration**
The minimum VAD speaking duration. Default value: "0.1"

**vad_min_silence_duration**
The minimum VAD silence duration. Default value: "0.5"

8.122.1 Examples

Run a transcription with srt file generation:
```shell
    ffmpeg -i input.mp4 -vn -af "whisper=model=../whisper.cpp/models/ggml-base.en.bin\
    :language=en\
    :queue=3\
    :destination=output.srt\
    :format=srt" -f null -
```


Run a transcription and send the output in JSON format to an HTTP service:
```shell
    ffmpeg -i input.mp4 -vn -af "whisper=model=../whisper.cpp/models/ggml-base.en.bin\
    :language=en\
    :queue=3\
    :destination=http\\://localhost\\:3000\
    :format=json' -f null -
```

Transcribe the microphone input using the VAD option:
```shell
ffmpeg -loglevel warning -f pulse -i default \
-af 'highpass=f=200,lowpass=f=3000,whisper=model=../whisper.cpp/models/ggml-medium.bin\
:language=en\
:queue=10\
:destination=-\
:format=json\
:vad_model=../whisper.cpp/models/ggml-silero-v5.1.2.bin' -f null -

```