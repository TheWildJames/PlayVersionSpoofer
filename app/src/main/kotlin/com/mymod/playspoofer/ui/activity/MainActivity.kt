package com.mymod.playspoofer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mymod.playspoofer.R
import com.mymod.playspoofer.ui.composable.PreferenceKeys
import com.mymod.playspoofer.ui.composable.rememberStringSharedPreference
import com.mymod.playspoofer.ui.theme.PlaySpooferTheme
import com.mymod.playspoofer.xposed.statusIsModuleActivated

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // 标题卡片
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
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
                    ) {                        Text(
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
    
    // Update inputs when preferences change
    LaunchedEffect(versionCodePref.value, versionNamePref.value) {
        versionCodeInput = versionCodePref.value
        versionNameInput = versionNamePref.value
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                OutlinedButton(
                    onClick = {
                        versionCodeInput = PreferenceKeys.DEFAULT_VERSION_CODE
                        versionNameInput = PreferenceKeys.DEFAULT_VERSION_NAME
                        versionCodePref.setValue(PreferenceKeys.DEFAULT_VERSION_CODE)
                        versionNamePref.setValue(PreferenceKeys.DEFAULT_VERSION_NAME)
                        showSavedMessage = true
                    }
                ) {
                    Text(stringResource(R.string.reset_defaults))
                }
                
                // Save Button
                Button(
                    onClick = {
                        val codeToSave = versionCodeInput.ifBlank { PreferenceKeys.DEFAULT_VERSION_CODE }
                        val nameToSave = versionNameInput.ifBlank { PreferenceKeys.DEFAULT_VERSION_NAME }
                        versionCodePref.setValue(codeToSave)
                        versionNamePref.setValue(nameToSave)
                        showSavedMessage = true
                    }
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
