package com.mymod.playspoofer.ui.composable

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

// Constants for preference keys
object PreferenceKeys {
    const val PREFS_NAME = "spoofer_settings"
    const val KEY_VERSION_CODE = "version_code"
    const val KEY_VERSION_NAME = "version_name"
    const val DEFAULT_VERSION_CODE = "99999999"
    const val DEFAULT_VERSION_NAME = "999.999.999"
}

/**
 * Get SharedPreferences with world-readable mode for Xposed compatibility
 */
@Suppress("DEPRECATION")
fun getWorldReadablePrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PreferenceKeys.PREFS_NAME, Context.MODE_WORLD_READABLE)
}

@Composable
fun rememberStringSharedPreference(
    key: String,
    defaultValue: String,
): StringSharedPreference {
    val context = LocalContext.current
    val preference = remember(key) {
        StringSharedPreference(context, key, defaultValue)
    }

    DisposableEffect(preference) {
        onDispose {
            preference.clean()
        }
    }

    return preference
}

class StringSharedPreference(
    context: Context,
    private val key: String,
    private val defaultValue: String,
) {
    @Suppress("DEPRECATION")
    private val sharedPreferences = runCatching {
        context.getSharedPreferences(PreferenceKeys.PREFS_NAME, Context.MODE_WORLD_READABLE)
    }.getOrNull()

    private val listener = OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
        if (changedKey != key) {
            return@OnSharedPreferenceChangeListener
        }
        value = sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    init {
        sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    private val prefsValue get() = sharedPreferences?.getString(key, defaultValue) ?: defaultValue

    var value by mutableStateOf(prefsValue)
        private set

    fun setValue(newValue: String) {
        CoroutineScope(Dispatchers.IO).launch {
            sharedPreferences?.edit(commit = true) { putString(key, newValue) }
            value = newValue
        }
    }

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: String) {
        setValue(value)
    }

    fun clean() {
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

@Composable
fun rememberBooleanSharedPreference(
    preferenceFileKey: String? = null,
    mode: Int = Context.MODE_PRIVATE,
    key: String,
    defaultValue: Boolean,
    beforeSet: ((Boolean) -> Boolean)? = null,
    afterSet: ((Boolean) -> Unit)? = null,
): BooleanSharedPreference {
    val context = LocalContext.current
    val preference = remember(key) {
        BooleanSharedPreference(
            context, preferenceFileKey, mode, key, defaultValue, beforeSet, afterSet
        )
    }

    DisposableEffect(preference) {
        onDispose {
            preference.clean()
        }
    }

    return preference
}

class BooleanSharedPreference(
    context: Context,
    preferenceFileKey: String? = null,
    mode: Int = Context.MODE_PRIVATE,
    private val key: String,
    private val defaultValue: Boolean,
    private val beforeSet: ((Boolean) -> Boolean)? = null,
    private val afterSet: ((Boolean) -> Unit)? = null,
) {
    private val sharedPreferences = runCatching {
        context.getSharedPreferences(
            preferenceFileKey ?: (context.packageName + "_preferences"), mode
        )
    }.getOrNull()

    private val listener = OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
        if (changedKey != key) {
            return@OnSharedPreferenceChangeListener
        }

        value = sharedPreferences.getBoolean(key, defaultValue)
    }

    init {
        sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    private val prefsValue get() = sharedPreferences?.getBoolean(key, defaultValue) ?: defaultValue

    private var value by mutableStateOf(prefsValue)

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefsValue = beforeSet?.invoke(value) ?: value
            sharedPreferences?.edit(commit = true) { putBoolean(key, prefsValue) }
            this@BooleanSharedPreference.value = prefsValue
            afterSet?.invoke(prefsValue)
        }
    }

    fun clean() {
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
