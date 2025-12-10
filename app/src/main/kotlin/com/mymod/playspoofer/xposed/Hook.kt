package com.mymod.playspoofer.xposed

import android.content.pm.PackageInfo
import androidx.annotation.Keep
import com.mymod.playspoofer.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File

@Keep
class Hook : IXposedHookLoadPackage {
    companion object {
        private const val PLAY_STORE_PKG = "com.android.vending"
        private const val CONFIG_FILE = "/data/local/tmp/playspoofer_config.txt"
        private const val FALLBACK_VERSION_CODE = 99999999L
        private const val FALLBACK_VERSION_NAME = "999.999.999"

        /** 标记是否已经在第一次 Hook 成功时打印过日志 */
        @Volatile
        private var hasHookedPlayStore = false
        
        // Cached config values
        private var cachedVersionCode: Long? = null
        private var cachedVersionName: String? = null
        private var lastConfigRead: Long = 0
    }
    
    private fun readConfig() {
        // Re-read config every 5 seconds at most
        val now = System.currentTimeMillis()
        if (now - lastConfigRead < 5000 && cachedVersionCode != null) {
            return
        }
        lastConfigRead = now
        
        try {
            val configFile = File(CONFIG_FILE)
            if (configFile.exists() && configFile.canRead()) {
                val lines = configFile.readLines()
                for (line in lines) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        when (parts[0].trim()) {
                            "version_code" -> cachedVersionCode = parts[1].trim().toLongOrNull()
                            "version_name" -> cachedVersionName = parts[1].trim()
                        }
                    }
                }
                Log.i("Config loaded: code=${cachedVersionCode}, name=${cachedVersionName}")
            } else {
                Log.i("Config file not found or not readable, using defaults")
            }
        } catch (e: Exception) {
            Log.e("Failed to read config: ${e.message}", e)
        }
    }
    
    private fun getVersionCode(): Long {
        readConfig()
        return cachedVersionCode ?: FALLBACK_VERSION_CODE
    }
    
    private fun getVersionName(): String {
        readConfig()
        return cachedVersionName ?: FALLBACK_VERSION_NAME
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 1. 针对自身模块的激活状态 Hook
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.mymod.playspoofer.xposed.ModuleStatusKt",
                    lpparam.classLoader,
                    "isModuleActivated",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                            Log.i("Module activation status forced to true")
                        }
                    }
                )
                Log.i("PlaySpoofer 模块已成功加载")
            } catch (e: Throwable) {
                Log.e("Failed to hook module status: ${e.message}", e)
            }
            return
        }

        // 2. 只对 Google Play Store 生效
        if (lpparam.packageName != PLAY_STORE_PKG) return

        // 只在第一次 Hook Play Store 时输出“开始 Hook [进程名]”日志
        if (!hasHookedPlayStore) {
            Log.i("开始 Hook 进程：${lpparam.packageName}")
        }

        // Hook getPackageInfo(String, int)
        hookGetPackageInfo(
            lpparam,
            arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)
        )
        // Hook getPackageInfo(VersionedPackage, int)
        try {
            val versionedClass = Class.forName("android.content.pm.VersionedPackage")
            hookGetPackageInfo(
                lpparam,
                arrayOf<Class<*>>(versionedClass, Int::class.javaPrimitiveType!!)
            )
        } catch (e: ClassNotFoundException) {
            Log.i("VersionedPackage 类不存在，跳过第二个 Hook")
        }
    }

    /**
     * 提炼出的公共方法，支持两种签名：
     *   - getPackageInfo(String, int)
     *   - getPackageInfo(VersionedPackage, int)
     */
    private fun hookGetPackageInfo(
        lpparam: LoadPackageParam,
        paramTypes: Array<Class<*>>
    ) {
        val methodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // 获取第一个参数：可能是 String，也可能是 VersionedPackage
                val pkgArg = param.args[0]
                val pkgName = when (pkgArg) {
                    is String -> pkgArg
                    else -> XposedHelpers.callMethod(pkgArg, "getPackageName") as? String ?: return
                }
                if (pkgName != PLAY_STORE_PKG) return

                // 拿到原始 PackageInfo 对象
                (param.result as? PackageInfo)?.let { pkgInfo ->
                    if (!hasHookedPlayStore) {
                        // 第一次进入这里：打印原始版本→伪装版本日志，并设置标志
                        logVersion("原始版本", pkgInfo)
                        modifyPackageInfo(pkgInfo)
                        logVersion("已伪装版本", pkgInfo)
                        hasHookedPlayStore = true
                    } else {
                        // 后续所有调用：只做版本号修改，不再打印日志
                        modifyPackageInfo(pkgInfo)
                    }
                    // 必须重新赋值回去
                    param.result = pkgInfo
                }
            }
        }

        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            lpparam.classLoader,
            "getPackageInfo",
            *paramTypes,
            methodHook
        )
    }

    /**
     * 打印版本号信息
     */
    private fun logVersion(tagPrefix: String, packageInfo: PackageInfo) {
        val versionInfo = buildString {
            append("longVersionCode=").append(packageInfo.longVersionCode)
            append(", versionName=").append(packageInfo.versionName)
        }
        Log.i("$tagPrefix -> $versionInfo")
    }

    /**
     * 强制修改 PackageInfo 中的版本号字段为自定义值
     */
    private fun modifyPackageInfo(packageInfo: PackageInfo) {
        packageInfo.apply {
            longVersionCode = getVersionCode()
            versionName = getVersionName()
        }
    }
}
