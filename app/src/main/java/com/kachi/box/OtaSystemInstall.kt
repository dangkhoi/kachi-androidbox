package com.kachi.box

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Log
import androidx.core.content.FileProvider
import com.kachi.box.launcher.NoShellFallback
import com.kachi.box.system.PackageQueries
import java.io.File
import java.security.MessageDigest

/**
 * ═══ Android box B3 — cài OTA bằng TRÌNH CÀI APP CỦA HỆ THỐNG khi không có kênh shell ═════════════════════════
 * Spec `androidbox-plan.html` §4.2 (d). Đường [UpdateChecker.install] (dadb `pm install -r`) cần kênh adb loopback mà
 * nhiều Android box không có [CHƯA BIẾT — tuỳ máy]; câu cũ "APK đã tải ở: <filesDir>/update/…" chỉ tới thư mục RIÊNG
 * của app — người dùng không mở được. Trình cài hệ thống: `ACTION_VIEW` + URI `FileProvider` (chỉ `update/`,
 * `res/xml/ota_paths.xml`) + quyền đọc URI cho đúng ý-định này. Lần đầu hệ thống tự hỏi "cho phép cài từ nguồn này"
 * (`REQUEST_INSTALL_PACKAGES`).
 *
 * Hai bước, hai luồng (review Pass 2 [P2]): [verify] trên luồng NỀN (đọc APK ~45 MB — không làm trên luồng chính) chặn
 * APK khác GÓI ([NoShellFallback.archiveMatches]) hoặc khác NGƯỜI KÝ ([NoShellFallback.signersMatch]) trước khi người
 * dùng thấy nút "Cài"; [launch] trên luồng CHÍNH chỉ mở trình cài. Trình cài vẫn tự từ chối khác chữ ký — kiểm ở đây để
 * nói câu rõ thay cho "Chưa cài đặt ứng dụng" của hệ thống.
 */
object OtaSystemInstall {

    private const val TAG = "UpdateChecker"

    /**
     * Luồng NỀN. `null` = cài được; khác `null` = câu báo (APK đã bị xoá). Người ký không đọc được (một bên rỗng) ⇒ để
     * trình cài hệ thống quyết — không chặn một bản cập nhật hợp lệ chỉ vì một lượt đọc hỏng.
     */
    fun verify(ctx: Context, apk: File): String? {
        val own = ctx.packageName
        val pm = ctx.packageManager
        val archive = runCatching {
            PackageQueries.archiveInfo(pm, apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        }.getOrNull()
        val archivePkg = archive?.packageName
        if (!NoShellFallback.archiveMatches(archivePkg, own)) {
            Log.w(TAG, "trình cài hệ thống: APK là gói '$archivePkg', không phải $own — KHÔNG cài")
            runCatching { apk.delete() }
            return Lang.f("APK tải về không phải của Kachi ({0}) — không cài", "the downloaded APK is not Kachi ({0}) — not installed", archivePkg ?: "?")
        }
        val ownInfo = runCatching { PackageQueries.packageInfo(pm, own, PackageManager.GET_SIGNING_CERTIFICATES) }.getOrNull()
        val ownSigners = digests(ownInfo?.signingInfo?.apkContentsSigners) + digests(ownInfo?.signingInfo?.signingCertificateHistory)
        if (!NoShellFallback.signersMatch(digests(archive?.signingInfo?.apkContentsSigners), ownSigners)) {
            Log.w(TAG, "trình cài hệ thống: APK ký bằng khoá khác bản đang cài — KHÔNG cài")
            runCatching { apk.delete() }
            return Lang.t("APK tải về ký bằng khoá khác — không cài", "the downloaded APK has a different signature — not installed")
        }
        return null
    }

    /** Luồng CHÍNH (mở activity), SAU [verify]. Trả câu cho dòng trạng thái của nút cập nhật. */
    fun launch(activity: Activity, apk: File): String = try {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.ota", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        Log.i(TAG, "trình cài hệ thống: đã mở cho ${apk.name}")
        Lang.t(
            "không có kênh shell — đã mở trình cài của máy, bấm Cài (lần đầu cho phép cài từ Kachi)",
            "no shell channel — opened the device installer, tap Install (first time: allow installs from Kachi)",
        )
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "trình cài hệ thống: máy không có trình cài app")
        Lang.t("máy không có trình cài app — không cài được bản mới", "the device has no package installer — cannot install")
    } catch (e: RuntimeException) {   // IllegalArgumentException (FileProvider) · SecurityException
        Log.w(TAG, "trình cài hệ thống: ${e.javaClass.simpleName}: ${e.message}")
        Lang.f("không mở được trình cài của máy ({0})", "could not open the device installer ({0})", e.javaClass.simpleName)
    }

    /** SHA-256 (hex) của từng chứng chỉ ký; `null` ⇒ tập rỗng. */
    private fun digests(sigs: Array<Signature>?): Set<String> = sigs.orEmpty().mapTo(HashSet()) { s ->
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
