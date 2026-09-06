package com.wavelength.music.activation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
fun AdminActivationScreen(
    onBack: () -> Unit,
    viewModel: AdminActivationViewModel = hiltViewModel()
) {
    val authenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activation Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!authenticated) {
            AdminLoginForm(
                modifier = Modifier.padding(padding),
                isLoading = isLoading,
                message = message,
                onLogin = viewModel::login,
                onDismissMessage = viewModel::clearMessage
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = viewModel::loadRequests, enabled = !isLoading) {
                            Text("Refresh")
                        }
                        TextButton(onClick = viewModel::logout) { Text("Log out") }
                    }
                }
                message?.let { text ->
                    item {
                        Text(text, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
                    }
                }
                if (isLoading) item { CircularProgressIndicator() }
                if (!isLoading && requests.isEmpty()) {
                    item { Text("No activation requests yet.") }
                }
                items(requests, key = { it.id ?: it.email }) { record ->
                    AdminRequestCard(
                        record = record,
                        enabled = !isLoading,
                        onApprove = { days -> record.id?.let { viewModel.approve(it, days) } },
                        onReject = { record.id?.let(viewModel::reject) },
                        onRevoke = { record.id?.let(viewModel::revoke) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminLoginForm(
    modifier: Modifier,
    isLoading: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit,
    onDismissMessage: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Admin sign in", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Use the admin email and the separate admin password configured on your activation backend. Do not enter your Gmail account password here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Admin email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Activation admin password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onLogin(email, password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator() else Text("Sign in")
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismissMessage) { Text("Dismiss") }
        }
    }
}

@Composable
private fun AdminRequestCard(
    record: ActivationRecord,
    enabled: Boolean,
    onApprove: (Int) -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit
) {
    var duration by remember(record.id) { mutableIntStateOf(record.durationDays ?: 30) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(record.email, style = MaterialTheme.typography.titleMedium)
            Text("Status: ${record.status.name.replace('_', ' ')}")
            record.requestedAt?.let {
                Text("Requested: ${DateFormat.getDateTimeInstance().format(Date(it))}")
            }
            record.expirationDate?.let {
                Text("Expires: ${DateFormat.getDateTimeInstance().format(Date(it))}")
            }

            if (record.status == ActivationStatus.PENDING || record.status == ActivationStatus.REJECTED || record.status == ActivationStatus.EXPIRED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { days ->
                        OutlinedButton(
                            onClick = { duration = days },
                            enabled = enabled
                        ) {
                            Text(if (duration == days) "✓ $days days" else "$days days")
                        }
                    }
                }
                Button(onClick = { onApprove(duration) }, enabled = enabled) {
                    Text("Approve")
                }
                OutlinedButton(onClick = onReject, enabled = enabled) {
                    Text("Reject")
                }
            }

            if (record.status == ActivationStatus.ACTIVE) {
                OutlinedButton(onClick = onRevoke, enabled = enabled) {
                    Text("Deactivate")
                }
            }
        }
    }
}
