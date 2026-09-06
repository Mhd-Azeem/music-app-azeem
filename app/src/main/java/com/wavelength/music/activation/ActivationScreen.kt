package com.wavelength.music.activation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
fun ActivationScreen(
    onBack: () -> Unit,
    onAdminClick: () -> Unit = {},
    viewModel: ActivationViewModel = hiltViewModel()
) {
    val activation by viewModel.activation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var email by remember(activation.email) { mutableStateOf(activation.email) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val hasSession = activation.email.isNotBlank()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Activation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onAdminClick) { Text("Admin") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasSession) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            viewModel.clearMessage()
                        },
                        text = { Text("Register") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            viewModel.clearMessage()
                        },
                        text = { Text("Login") }
                    )
                }

                Text(
                    if (selectedTab == 0) {
                        "Register your Gmail to request access."
                    } else {
                        "Already registered? Enter the same Gmail to log in."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gmail address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.requestActivation(email)
                        } else {
                            viewModel.loginExisting(email)
                        }
                    },
                    enabled = !isLoading && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(if (selectedTab == 0) "Request Access" else "Login")
                    }
                }
            } else {
                Text(
                    text = statusTitle(activation.status),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(activation.email, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "This email stays logged in on this device until you tap Log out.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = statusDescription(activation, viewModel.getRemainingDays()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                activation.expirationDate?.let { expiration ->
                    Text(
                        text = "Expires: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(expiration))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                when (activation.status) {
                    ActivationStatus.PENDING, ActivationStatus.ACTIVE -> {
                        Button(
                            onClick = viewModel::refresh,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) CircularProgressIndicator() else Text("Refresh status")
                        }
                    }

                    ActivationStatus.NOT_ACTIVATED,
                    ActivationStatus.EXPIRED,
                    ActivationStatus.REJECTED,
                    ActivationStatus.REVOKED -> {
                        Button(
                            onClick = { viewModel.requestActivation(activation.email) },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) CircularProgressIndicator() else Text("Request Access Again")
                        }
                    }
                }

                OutlinedButton(
                    onClick = viewModel::logout,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log out")
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
            }
        }
    }
}

private fun statusTitle(status: ActivationStatus): String = when (status) {
    ActivationStatus.NOT_ACTIVATED -> "Not activated"
    ActivationStatus.PENDING -> "Pending approval"
    ActivationStatus.ACTIVE -> "Email activated"
    ActivationStatus.EXPIRED -> "Activation expired"
    ActivationStatus.REJECTED -> "Activation rejected"
    ActivationStatus.REVOKED -> "Activation revoked"
}

private fun statusDescription(record: ActivationRecord, remainingDays: Long): String = when (record.status) {
    ActivationStatus.NOT_ACTIVATED -> "This email has not been activated."
    ActivationStatus.PENDING -> "Your access request is being reviewed."
    ActivationStatus.ACTIVE -> if (record.expirationDate == null) {
        "Lifetime access is active."
    } else {
        "Access is active. Expires in $remainingDays day${if (remainingDays == 1L) "" else "s"}."
    }
    ActivationStatus.EXPIRED -> "Your email activation has expired. Request access again to restore online music."
    ActivationStatus.REJECTED -> "Your access request was not approved."
    ActivationStatus.REVOKED -> "This activation has been deactivated."
}
