package com.themarknet.aether.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.themarknet.aether.AetherApp
import com.themarknet.aether.data.db.entities.MessageEntity
import com.themarknet.aether.data.db.entities.ReactionEntity
import com.themarknet.aether.rcs.RcsManager
import com.themarknet.aether.sms.SmsHelper
import com.themarknet.aether.ui.components.MessageBubble
import com.themarknet.aether.ui.components.ReactionPicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    threadId: Long,
    address: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val messageDao = app.database.messageDao()
    val reactionDao = app.database.reactionDao()
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var showReactionPickerFor by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(threadId) {
        messageDao.getMessagesForThread(threadId).collectLatest {
            messages = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(address)
                        val rcsCapable = RcsManager.isRcsCapable(context)
                        val rcsEnabled = RcsManager.isRcsEnabled(context)
                        Text(
                            text = if (rcsEnabled && rcsCapable) "RCS chat features: ON (stub)" else "RCS: OFF / not available",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rcsEnabled && rcsCapable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Simple toggle for the RCS attempt
                    val rcsEnabled = RcsManager.isRcsEnabled(context)
                    TextButton(onClick = {
                        RcsManager.setRcsEnabled(context, !rcsEnabled)
                    }) {
                        Text(if (rcsEnabled) "Disable RCS" else "Enable RCS (experimental)")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            // Use RCS manager if enabled (currently falls back gracefully)
                            val success = RcsManager.sendMessage(context, address, messageText)
                            if (success) {
                                scope.launch {
                                    val newMsg = MessageEntity(
                                        id = System.currentTimeMillis(),
                                        threadId = threadId,
                                        address = address,
                                        body = messageText,
                                        date = System.currentTimeMillis(),
                                        type = 2 // Sent
                                    )
                                    messageDao.insertMessage(newMsg)
                                }
                                messageText = ""
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                val reactions by produceState(initialValue = emptyList<ReactionEntity>()) {
                    reactionDao.getReactionsForMessage(message.id).collectLatest { value = it }
                }

                MessageBubble(
                    message = message,
                    reactions = reactions,
                    onLongPress = {
                        showReactionPickerFor = message.id
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (showReactionPickerFor != null) {
            ReactionPicker(
                onEmojiSelected = { emoji ->
                    val msgId = showReactionPickerFor!!
                    scope.launch {
                        // Toggle reaction
                        val existing = reactions.find { it.emoji == emoji }
                        if (existing != null) {
                            reactionDao.deleteReaction(msgId, emoji)
                        } else {
                            reactionDao.insertReaction(
                                ReactionEntity(messageId = msgId, emoji = emoji)
                            )
                        }
                    }
                    showReactionPickerFor = null
                },
                onDismiss = { showReactionPickerFor = null }
            )
        }
    }
}