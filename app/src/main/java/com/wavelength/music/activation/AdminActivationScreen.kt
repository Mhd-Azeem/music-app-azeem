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
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                        onRevoke = { record.id?.let(viewModel::revoke) },
                        onResetDevice = { record.id?.let(viewModel::resetDevice) },
                        onDelete = { record.id?.let(viewModel::deleteRevoked) }
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
    onApprove: (Int?) -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit,
    onResetDevice: () -> Unit,
    onDelete: () -> Unit
) {
    var duration by remember(record.id) { mutableIntStateOf(record.durationDays ?: 30) }
    var confirmationAction by remember(record.id) { mutableStateOf<AdminConfirmationAction?>(null) }

    confirmationAction?.let { action ->
        SwipeConfirmationDialog(
            action = action,
            onConfirm = {
                when (action) {
                    AdminConfirmationAction.DEACTIVATE -> onRevoke()
                    AdminConfirmationAction.RESET_DEVICE -> onResetDevice()
                    AdminConfirmationAction.DELETE_HISTORY -> onDelete()
                }
                confirmationAction = null
            },
            onDismiss = { confirmationAction = null }
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(record.email, style = MaterialTheme.typography.titleMedium)
            Text("Status: ${record.status.name.replace('_', ' ')}")
            Text(if (record.deviceBound) "Device: Linked" else "Device: Not linked")
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
                OutlinedButton(
                    onClick = { duration = 0 },
                    enabled = enabled
                ) {
                    Text(if (duration == 0) "✓ Lifetime" else "Lifetime")
                }
                Button(onClick = { onApprove(duration.takeIf { it != 0 }) }, enabled = enabled) {
                    Text(if (duration == 0) "Approve Lifetime" else "Approve")
                }
                OutlinedButton(onClick = onReject, enabled = enabled) {
                    Text("Reject")
                }
            }

            if (record.deviceBound) {
                OutlinedButton(onClick = { confirmationAction = AdminConfirmationAction.RESET_DEVICE }, enabled = enabled) {
                    Text("Reset Device")
                }
            }

            if (record.status == ActivationStatus.ACTIVE) {
                OutlinedButton(onClick = { confirmationAction = AdminConfirmationAction.DEACTIVATE }, enabled = enabled) {
                    Text("Deactivate")
                }
            }

            if (record.status == ActivationStatus.REVOKED) {
                Button(onClick = { confirmationAction = AdminConfirmationAction.DELETE_HISTORY }, enabled = enabled) {
                    Text("Delete from history")
                }
            }
        }
    }
}


private enum class AdminConfirmationAction(val phrase: String) {
    DEACTIVATE("deactivate this activation"),
    RESET_DEVICE("reset this device"),
    DELETE_HISTORY("delete this revoked address from app history")
}

@Composable
private fun SwipeConfirmationDialog(
    action: AdminConfirmationAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Are you sure you want to ${action.phrase}?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            SwipeToConfirm(
                onConfirmed = onConfirm
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@Composable
private fun SwipeToConfirm(
    onConfirmed: () -> Unit
) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var maxOffsetPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size ->
                // 50.dp handle with 4.dp padding on each side.
                val handleWithPaddingPx = size.height.toFloat()
                maxOffsetPx = (size.width - handleWithPaddingPx).coerceAtLeast(0f)
                offsetPx = offsetPx.coerceIn(0f, maxOffsetPx)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Swipe to confirm  →",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )

        Surface(
            modifier = Modifier
                .padding(4.dp)
                .size(50.dp)
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (maxOffsetPx > 0f && offsetPx >= maxOffsetPx * 0.85f) {
                            offsetPx = maxOffsetPx
                            onConfirmed()
                        } else {
                            val start = offsetPx
                            scope.launch {
                                animate(
                                    initialValue = start,
                                    targetValue = 0f
                                ) { value, _ ->
                                    offsetPx = value
                                }
                            }
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
