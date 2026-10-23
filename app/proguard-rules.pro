# NSFlip R8 rules. SPDX-License-Identifier: GPL-3.0-or-later

# Keep Shizuku binder entry points referenced via AIDL/RI
-keep class moe.shizuku.api.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep class android.content.IContentProvider { *; }
