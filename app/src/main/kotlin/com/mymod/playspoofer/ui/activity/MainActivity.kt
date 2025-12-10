package com.mymod.playspoofer.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mymod.playspoofer.R
import com.mymod.playspoofer.ui.composable.PreferenceKeys
import com.mymod.playspoofer.ui.theme.PlaySpooferTheme
import com.mymod.playspoofer.util.ConfigManager
import com.mymod.playspoofer.util.ReleaseInfo
import com.mymod.playspoofer.util.UpdateChecker
import com.mymod.playspoofer.xposed.statusIsModuleActivated
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaySpooferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        // Update checker card
        UpdateCheckerCard()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 标题卡片
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "v${UpdateChecker.currentVersionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_screen_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 模块状态
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isActivated = statusIsModuleActivated
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActivated) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isActivated) stringResource(R.string.status_activated) else stringResource(R.string.status_not_activated),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActivated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                    Text(
                        text = if (isActivated) {
                            stringResource(R.string.status_activated_desc)
                        } else {
                            stringResource(R.string.status_not_activated_desc)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 使用说明
                Text(
                    text = stringResource(R.string.usage_instructions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.usage_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.step_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.step_2),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.step_3),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom Version Settings Card
        VersionSettingsCard()
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UpdateCheckerCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasChecked by remember { mutableStateOf(false) }
    
    // Get pre-release preference
    val includePrereleases = remember { SettingsPrefs.getIncludePrereleases(context) }
    
    // Function to check for updates
    fun checkUpdates() {
        scope.launch {
            isChecking = true
            errorMessage = null
            val includePrerelease = SettingsPrefs.getIncludePrereleases(context)
            UpdateChecker.checkForUpdates(context, includePrerelease).fold(
                onSuccess = { release ->
                    updateInfo = release
                    errorMessage = null
                },
                onFailure = { error ->
                    errorMessage = error.message
                }
            )
            isChecking = false
            hasChecked = true
        }
    }
    
    // Auto-check on first load
    LaunchedEffect(Unit) {
        checkUpdates()
    }
    
    // Only show card if there's an update, error, or checking
    if (isChecking || updateInfo != null || (hasChecked && errorMessage == null && updateInfo == null)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    updateInfo != null -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.update_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (updateInfo != null) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    
                    if (!isChecking) {
                        TextButton(onClick = { checkUpdates() }) {
                            Text(stringResource(R.string.update_check))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when {
                    isChecking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.update_checking),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    updateInfo != null -> {
                        val release = updateInfo!!
                        Text(
                            text = stringResource(
                                R.string.update_available,
                                release.name
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (release.isPrerelease) {
                            Text(
                                text = stringResource(R.string.update_prerelease),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dismiss button
                            OutlinedButton(
                                onClick = {
                                    UpdateChecker.dismissRelease(context, release.tagName, release.publishedAt)
                                    updateInfo = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.update_dismiss))
                            }
                            
                            // Download button
                            if (release.downloadUrl != null) {
                                Button(
                                    onClick = {
                                        UpdateChecker.dismissRelease(context, release.tagName, release.publishedAt)
                                        UpdateChecker.downloadApk(context, release)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.update_download))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        UpdateChecker.dismissRelease(context, release.tagName, release.publishedAt)
                                        UpdateChecker.openReleasePage(context, release)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.update_view))
                                }
                            }
                        }
                    }
                    hasChecked && updateInfo == null && errorMessage == null -> {
                        Text(
                            text = stringResource(R.string.update_up_to_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = stringResource(R.string.update_error, errorMessage ?: "Unknown"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionSettingsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Detect Play Store's actual version as default
    val playStoreVersion = remember { ConfigManager.detectPlayStoreVersion(context) }
    val detectedCode = playStoreVersion?.versionCode?.toString() ?: PreferenceKeys.FALLBACK_VERSION_CODE
    val detectedName = playStoreVersion?.versionName ?: PreferenceKeys.FALLBACK_VERSION_NAME
    
    // Current saved config
    var savedCode by remember { mutableStateOf<String?>(null) }
    var savedName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Input fields
    var versionCodeInput by remember { mutableStateOf("") }
    var versionNameInput by remember { mutableStateOf("") }
    
    // Status messages
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    
    // Load current config on start
    LaunchedEffect(Unit) {
        val config = ConfigManager.readConfig()
        savedCode = config?.versionCode
        savedName = config?.versionName
        versionCodeInput = config?.versionCode ?: PreferenceKeys.MAX_VERSION_CODE
        versionNameInput = config?.versionName ?: PreferenceKeys.MAX_VERSION_NAME
        isLoading = false
    }
    
    // Track if there are unsaved changes
    val hasUnsavedChanges = !isLoading && (versionCodeInput != (savedCode ?: "") || versionNameInput != (savedName ?: ""))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Show detected Play Store version
            Text(
                text = stringResource(R.string.detected_version, detectedCode, detectedName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Show current spoofed values if set
            if (savedCode != null && savedName != null) {
                Text(
                    text = stringResource(R.string.current_values, savedCode!!, savedName!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Version Code Input
            OutlinedTextField(
                value = versionCodeInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        versionCodeInput = newValue
                    }
                },
                label = { Text(stringResource(R.string.version_code_label)) },
                placeholder = { Text(PreferenceKeys.MAX_VERSION_CODE) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Version Name Input
            OutlinedTextField(
                value = versionNameInput,
                onValueChange = { versionNameInput = it },
                label = { Text(stringResource(R.string.version_name_label)) },
                placeholder = { Text(PreferenceKeys.MAX_VERSION_NAME) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preset buttons row 1: Min and Max
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        versionCodeInput = PreferenceKeys.MIN_VERSION_CODE
                        versionNameInput = PreferenceKeys.MIN_VERSION_NAME
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.set_min))
                }
                
                OutlinedButton(
                    onClick = {
                        versionCodeInput = PreferenceKeys.MAX_VERSION_CODE
                        versionNameInput = PreferenceKeys.MAX_VERSION_NAME
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.set_max))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preset buttons row 2: Default (detected) and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        versionCodeInput = detectedCode
                        versionNameInput = detectedName
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.set_default))
                }
                
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val success = ConfigManager.deleteConfig()
                            if (success) {
                                savedCode = null
                                savedName = null
                                versionCodeInput = PreferenceKeys.MAX_VERSION_CODE
                                versionNameInput = PreferenceKeys.MAX_VERSION_NAME
                                statusMessage = context.getString(R.string.config_reset)
                                isError = false
                            } else {
                                statusMessage = context.getString(R.string.config_reset_failed)
                                isError = true
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.reset_config))
                }
            }
            
            // Show Save button only when there are unsaved changes
            if (hasUnsavedChanges) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val codeToSave = versionCodeInput.ifBlank { PreferenceKeys.MAX_VERSION_CODE }
                            val nameToSave = versionNameInput.ifBlank { PreferenceKeys.MAX_VERSION_NAME }
                            
                            val success = ConfigManager.writeConfig(codeToSave, nameToSave)
                            if (success) {
                                savedCode = codeToSave
                                savedName = nameToSave
                                statusMessage = context.getString(R.string.settings_saved)
                                isError = false
                            } else {
                                statusMessage = context.getString(R.string.save_failed)
                                isError = true
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.save_settings))
                    }
                }
            }
            
            // Status message
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                
                LaunchedEffect(statusMessage) {
                    kotlinx.coroutines.delay(3000)
                    statusMessage = null
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
