package com.mymod.playspoofer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.mymod.playspoofer.ui.composable.rememberStringSharedPreference
import com.mymod.playspoofer.ui.theme.PlaySpooferTheme
import com.mymod.playspoofer.util.ReleaseInfo
import com.mymod.playspoofer.util.UpdateChecker
import com.mymod.playspoofer.xposed.statusIsModuleActivated
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top
    ) {
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
                    Text(
                        text = "v${UpdateChecker.currentVersionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    
    // Auto-check on first load
    LaunchedEffect(Unit) {
        isChecking = true
        UpdateChecker.checkForUpdates(includePrerelease = true).fold(
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
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isChecking = true
                                    errorMessage = null
                                    UpdateChecker.checkForUpdates(includePrerelease = true).fold(
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
                        ) {
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
                            if (release.downloadUrl != null) {
                                Button(
                                    onClick = { UpdateChecker.downloadApk(context, release) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.update_download))
                                }
                            }
                            OutlinedButton(
                                onClick = { UpdateChecker.openReleasePage(context, release) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.update_view))
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
    val versionCodePref = rememberStringSharedPreference(
        key = PreferenceKeys.KEY_VERSION_CODE,
        defaultValue = PreferenceKeys.DEFAULT_VERSION_CODE
    )
    val versionNamePref = rememberStringSharedPreference(
        key = PreferenceKeys.KEY_VERSION_NAME,
        defaultValue = PreferenceKeys.DEFAULT_VERSION_NAME
    )
    
    var versionCodeInput by remember { mutableStateOf(versionCodePref.value) }
    var versionNameInput by remember { mutableStateOf(versionNamePref.value) }
    var showSavedMessage by remember { mutableStateOf(false) }
    
    // Track if there are unsaved changes
    val hasUnsavedChanges = versionCodeInput != versionCodePref.value || 
                           versionNameInput != versionNamePref.value
    
    // Update inputs when preferences change externally
    LaunchedEffect(versionCodePref.value, versionNamePref.value) {
        if (!hasUnsavedChanges) {
            versionCodeInput = versionCodePref.value
            versionNameInput = versionNamePref.value
        }
    }
    
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
            
            // Current saved values display
            Text(
                text = stringResource(R.string.current_values, versionCodePref.value, versionNamePref.value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Version Code Input
            OutlinedTextField(
                value = versionCodeInput,
                onValueChange = { newValue ->
                    // Only allow numeric input
                    if (newValue.all { it.isDigit() }) {
                        versionCodeInput = newValue
                    }
                },
                label = { Text(stringResource(R.string.version_code_label)) },
                placeholder = { Text(PreferenceKeys.DEFAULT_VERSION_CODE) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Version Name Input
            OutlinedTextField(
                value = versionNameInput,
                onValueChange = { versionNameInput = it },
                label = { Text(stringResource(R.string.version_name_label)) },
                placeholder = { Text(PreferenceKeys.DEFAULT_VERSION_NAME) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preset buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Set Default Button
                OutlinedButton(
                    onClick = {
                        versionCodeInput = PreferenceKeys.DEFAULT_VERSION_CODE
                        versionNameInput = PreferenceKeys.DEFAULT_VERSION_NAME
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.set_default))
                }
                
                // Set Max Button
                OutlinedButton(
                    onClick = {
                        versionCodeInput = PreferenceKeys.MAX_VERSION_CODE
                        versionNameInput = PreferenceKeys.MAX_VERSION_NAME
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.set_max))
                }
            }
            
            // Show Save button only when there are unsaved changes
            if (hasUnsavedChanges) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        val codeToSave = versionCodeInput.ifBlank { PreferenceKeys.DEFAULT_VERSION_CODE }
                        val nameToSave = versionNameInput.ifBlank { PreferenceKeys.DEFAULT_VERSION_NAME }
                        versionCodePref.updateValue(codeToSave)
                        versionNamePref.updateValue(nameToSave)
                        showSavedMessage = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_settings))
                }
            }
            
            if (showSavedMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Auto-hide the message after a short delay
                LaunchedEffect(showSavedMessage) {
                    kotlinx.coroutines.delay(2000)
                    showSavedMessage = false
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
