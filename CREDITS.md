# Credits — Kachi Android box

> Cập nhật 2026-10-09 (Android box B5): kiểm theo `app/build.gradle.kts` (`releaseRuntimeClasspath`) và
> `app/src/main/assets/`. Đã gỡ mục của phần chỉ-BYD (B2): `navopen.jar` (ghi khung dẫn đường xuống cụm) và hai tham
> chiếu clean-room HAL BYD (Overdrive-release · byd-dashcast). Bouncy Castle gỡ: không có trong classpath chạy, `dadb-2.0.0.jar`
> không đóng gói nó.

## dadb — embedded ADB client
[`dev.mobile:dadb`](https://github.com/mobile-dev-inc/dadb) 2.0.0 — ADB client thuần JVM, nhúng trong app để tự nối
`localhost:5555` → chạy lệnh đặc quyền dưới uid shell (no-root). Pure-JVM ADB client used for the adb loopback channel.
**License: Apache-2.0.**

## Bundled / transitive dependencies · Phụ thuộc đóng gói / gián tiếp

### okio
[`com.squareup.okio:okio`](https://github.com/square/okio) — I/O (buffer/streams) mà `dadb` dùng. I/O library used by
`dadb`. **License: Apache-2.0.**

### Kotlin standard library · kotlinx.coroutines
[`org.jetbrains.kotlin:kotlin-stdlib`](https://github.com/JetBrains/kotlin) ·
[`org.jetbrains.kotlinx:kotlinx-coroutines-android`](https://github.com/Kotlin/kotlinx.coroutines) — runtime Kotlin và
coroutine. Kotlin runtime and coroutines. **License: Apache-2.0.**

### AndroidX
`androidx.core:core` · `androidx.lifecycle:lifecycle-viewmodel-ktx` · `androidx.lifecycle:lifecycle-runtime-ktx`
([Android Jetpack](https://developer.android.com/jetpack)). **License: Apache-2.0.**

## Giọng nói tại máy · On-device voice

Âm thanh KHÔNG rời khỏi máy. Audio never leaves the device.

### sherpa-onnx (Android AAR)
[`k2-fsa/sherpa-onnx`](https://github.com/k2-fsa/sherpa-onnx) v1.13.8 — engine nhận dạng tiếng nói, phát hiện từ khoá
"Hey Kachi" và đọc phản hồi (TTS) tại máy; đóng gói `libsherpa-onnx-jni.so`. Speech recognition, keyword spotting and TTS
engine. **License: Apache-2.0.**

### ONNX Runtime
[`microsoft/onnxruntime`](https://github.com/microsoft/onnxruntime) — đóng gói `libonnxruntime.so` (đi kèm AAR
sherpa-onnx). Bundled with the sherpa-onnx AAR. **License: MIT © Microsoft Corporation.**

### Silero VAD
`app/src/main/assets/voice/silero_vad.onnx` — phát hiện tiếng nói để cắt khoảng lặng
([`snakers4/silero-vad`](https://github.com/snakers4/silero-vad), bản phân phối của sherpa-onnx `asr-models`).
Voice-activity detector, bundled in the APK. **License: MIT.**

### Mô hình nghe · Recognition model `sherpa-onnx-zipformer-vi-int8-2025-04-20`
Trọng số `zzasdf/viet_iter3_pseudo_label`, gói sherpa `csukuangfj/sherpa-onnx-zipformer-vi-int8-2025-04-20`. KHÔNG đóng
trong APK (chỉ bảng từ vựng BPE `zipformer-vi-2025-04-20.bpe_vocab.txt`): tải trong app từ Hugging Face (*Cài đặt › Giọng
nói*), kiểm sha256. Not bundled; downloaded in-app, sha256-verified. **License: Apache-2.0.**

### Mô hình từ khoá · Keyword model `sherpa-onnx-kws-zipformer-gigaspeech-3.3M` (int8)
Phát hiện "Hey Kachi"; đóng trong APK (`app/src/main/assets/voice/kws/`), bản tải lại nằm ở `voice/kws/` của repo
`dangkhoi/byd-kachi`. Wake-word spotter, bundled in the APK. **License: Apache-2.0 (k2-fsa).**

### Giọng đọc · TTS voice Piper `vi_VN-vais1000-medium`
Gói `vits-piper-vi_VN-vais1000-medium` (sherpa-onnx `tts-models`), tải trong app từ `voice/tts/piper-vi_VN-vais1000-medium/`
của repo `dangkhoi/byd-kachi` (chỉ đọc, ghim sha256). Dữ liệu huấn luyện VAIS-1000 Vietnamese Speech Synthesis Corpus (IEEE
DataPort) — **License: CC-BY-4.0** (ghi công bắt buộc · attribution required). Mô hình Piper
([`rhasspy/piper`](https://github.com/rhasspy/piper)) — MIT.

### espeak-ng data (đã tỉa phần tiếng Việt · Vietnamese subset)
`voice/tts/piper-vi_VN-vais1000-medium/espeak-ng-data/` (11 tệp: `phondata`, `phonindex`, `phontab`, `vi_dict`,
`lang/aav/vi*`…) — Piper dùng espeak-ng để chuyển chữ thành âm vị. Phonemizer data required by Piper.
[`espeak-ng/espeak-ng`](https://github.com/espeak-ng/espeak-ng) — **License: GPL-3.0-or-later.** Bản sao nguyên trạng,
không sửa nội dung; mã nguồn đầy đủ tại repo gốc. Redistributed unmodified; full source at the upstream repository.
