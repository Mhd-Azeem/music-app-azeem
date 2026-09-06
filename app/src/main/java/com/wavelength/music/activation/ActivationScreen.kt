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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
            Text(
                text = statusTitle(activation.status),
                style = MaterialTheme.typography.headlineSmall
            )

            if (activation.email.isNotBlank()) {
                Text(activation.email, style = MaterialTheme.typography.bodyLarge)
            }

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

            if (activation.status != ActivationStatus.ACTIVE) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Button(
                    onClick = { viewModel.requestActivation(email) },
                    enabled = !isLoading && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator()
                    else Text("Request Activation")
                }
            } else {
                TextButton(onClick = viewModel::refresh, enabled = !isLoading) {
                    Text("Refresh status")
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
    ActivationStatus.NOT_ACTIVATED -> "Enter your email address to request access to online music."
    ActivationStatus.PENDING -> "Your activation request is being reviewed."
    ActivationStatus.ACTIVE -> "Access is active. Expires in $remainingDays day${if (remainingDays == 1L) "" else "s"}."
    ActivationStatus.EXPIRED -> "Your email activation has expired. Request a new activation to restore online music access."
    ActivationStatus.REJECTED -> "Your activation request was not approved."
    ActivationStatus.REVOKED -> "This activation has been deactivated."
}
