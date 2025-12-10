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
    
    // These will be detected from Play Store - fallbacks if detection fails
    const val FALLBACK_VERSION_CODE = "99999999"
    const val FALLBACK_VERSION_NAME = "999.999.999"
    
    // Preset values
    const val MAX_VERSION_CODE = "9999999"
    const val MAX_VERSION_NAME = "999.999.999"
    const val MIN_VERSION_CODE = "0"
    const val MIN_VERSION_NAME = "0.0.0"
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
        _value = sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    init {
        sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    private val prefsValue get() = sharedPreferences?.getString(key, defaultValue) ?: defaultValue

    private var _value by mutableStateOf(prefsValue)
    
    val value: String get() = _value

    fun updateValue(newValue: String) {
        CoroutineScope(Dispatchers.IO).launch {
            sharedPreferences?.edit(commit = true) { putString(key, newValue) }
            _value = newValue
        }
    }

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = _value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: String) {
        updateValue(value)
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
