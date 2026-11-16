package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ContactEntity
import com.example.data.preferences.CallerBackdropMode
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.theme.LocalDotDialStyle
import com.example.ui.theme.LocalUiHapticPulse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCallingCardPickerSheet(
    contacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onSelectContactForCard: (ContactEntity) -> Unit,
    onClearContactCallingCard: (ContactEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = LocalDotDialStyle.current
    val hapticPulse = LocalUiHapticPulse.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val sorted = contacts.sortedWith(
            compareByDescending<ContactEntity> { !it.callingCardUri.isNullOrBlank() }
                .thenBy { it.name.lowercase() }
        )
        if (q.isBlank()) sorted
        else sorted.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("contact_calling_card_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Calling Card contact picker",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CALLING CARD • CHOOSE CONTACT",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap a person to pick an individual Calling Card image",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = {
                        hapticPulse()
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_calling_card_picker_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contact name or number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calling_card_contact_search")
            )

            if (filteredContacts.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, style.hairlineBorder)
                ) {
                    Text(
                        text = if (contacts.isEmpty()) {
                            "No contacts saved yet. Sync or add contacts in the Contacts tab first."
                        } else {
                            "No matching contacts found for \"$searchQuery\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.id },
                        contentType = { "calling_card_contact" }
                    ) { contact ->
                        val hasCard = !contact.callingCardUri.isNullOrBlank()
                        val isCardActive = contact.callerBackdropMode.equals(
                            CallerBackdropMode.CALLING_CARD.name,
                            ignoreCase = true
                        ) || contact.callerBackdropMode.equals(
                            CallerBackdropMode.CALLING_CARD.id,
                            ignoreCase = true
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    hapticPulse()
                                    onSelectContactForCard(contact)
                                }
                                .testTag("calling_card_contact_${contact.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (hasCard && isCardActive) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                } else {
                                    style.hairlineBorder
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ContactAvatarCircle(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 42.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name.ifBlank { contact.phoneNumber },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (hasCard) {
                                        Text(
                                            text = if (isCardActive) "CALLING CARD ACTIVE" else "CARD SAVED (TAP TO SET/CHANGE)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (hasCard && contact.callingCardUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(contact.callingCardUri),
                                            contentDescription = "${contact.name} calling card preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            hapticPulse()
                                            onClearContactCallingCard(contact)
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("clear_calling_card_${contact.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear calling card",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    DotMatrixTagChip(
                                        label = "PICK",
                                        active = false,
                                        onClick = { onSelectContactForCard(contact) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
