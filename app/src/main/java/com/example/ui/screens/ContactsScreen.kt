package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ContactEntity
import com.example.data.local.ContactNumbersCodec
import com.example.data.local.ContactPhoneNumber
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.CallerBackdropMode
import com.example.data.preferences.NewContactStorageMode
import com.example.data.system.T9Utils
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.LocalDotDialStyle
import com.example.ui.theme.LocalUiHapticPulse

private enum class ContactMediaTarget {
    PHOTO,
    BACKDROP_PHOTO,
    CALLING_CARD,
    VIDEO_BACKDROP
}

private fun effectiveContactBackdropMode(contact: ContactEntity): CallerBackdropMode {
    if (contact.callerBackdropMode.isNotBlank()) {
        return CallerBackdropMode.fromId(contact.callerBackdropMode)
    }
    return when {
        !contact.videoBackdropUri.isNullOrBlank() -> CallerBackdropMode.VIDEO
        !contact.backdropPhotoUri.isNullOrBlank() -> CallerBackdropMode.PHOTO
        !contact.callingCardUri.isNullOrBlank() -> CallerBackdropMode.CALLING_CARD
        else -> CallerBackdropMode.NONE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    searchQuery: String,
    vibrationPatterns: List<VibrationPatternEntity>,
    googleSyncEnabled: Boolean,
    showQuickActions: Boolean,
    discoveredGoogleAccounts: List<String>,
    syncedGoogleAccounts: Set<String>,
    newContactStorageMode: NewContactStorageMode = NewContactStorageMode.GOOGLE_ACCOUNT,
    targetGoogleContactAccount: String = "",
    onSearchChange: (String) -> Unit,
    onSyncDeviceContacts: () -> Unit,
    onToggleGoogleSync: (Boolean) -> Unit,
    onAddGoogleAccount: (String) -> Unit,
    onRemoveGoogleAccount: (String) -> Unit,
    onCallContact: (ContactEntity) -> Unit,
    onCallNumber: (String, ContactEntity) -> Unit,
    onSetDefaultNumber: (ContactEntity, ContactPhoneNumber) -> Unit,
    onSaveContact: (
        existingId: Long?,
        name: String,
        phone: String,
        company: String,
        email: String,
        notes: String,
        isFavorite: Boolean,
        photoUri: String?,
        callingCardUri: String?,
        videoUri: String?,
        vibrationId: Long,
        additionalNumbersJson: String
    ) -> Unit,
    onToggleFavorite: (ContactEntity) -> Unit,
    onToggleBlocked: (ContactEntity) -> Unit,
    onAssignPhoto: (ContactEntity, Uri?) -> Unit,
    onAssignCallingCard: (ContactEntity, Uri?) -> Unit,
    onAssignVideoBackdrop: (ContactEntity, Uri?) -> Unit,
    onAssignBackdropPhoto: (ContactEntity, Uri?) -> Unit = { _, _ -> },
    onAssignBackdropMode: (ContactEntity, CallerBackdropMode) -> Unit = { _, _ -> },
    onAssignVibrationPattern: (ContactEntity, Long) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onPinContactWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var detailContact by remember { mutableStateOf<ContactEntity?>(null) }
    var editingContact by remember { mutableStateOf<ContactEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showGoogleSyncSheet by remember { mutableStateOf(false) }

    var mediaTargetContact by remember { mutableStateOf<ContactEntity?>(null) }
    var mediaTargetKind by remember { mutableStateOf(ContactMediaTarget.PHOTO) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = mediaTargetContact
        if (target != null && uri != null) {
            when (mediaTargetKind) {
                ContactMediaTarget.PHOTO -> onAssignPhoto(target, uri)
                ContactMediaTarget.BACKDROP_PHOTO -> onAssignBackdropPhoto(target, uri)
                ContactMediaTarget.CALLING_CARD -> onAssignCallingCard(target, uri)
                ContactMediaTarget.VIDEO_BACKDROP -> Unit
            }
            detailContact = contacts.firstOrNull { it.id == target.id } ?: target
        }
        mediaTargetContact = null
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = mediaTargetContact
        if (target != null && uri != null) {
            onAssignVideoBackdrop(target, uri)
        }
        mediaTargetContact = null
    }

    val favorites = remember(contacts) { contacts.filter { it.isFavorite } }
    val patternNameById = remember(vibrationPatterns) {
        vibrationPatterns.associate { it.id to it.name }
    }
    val listState = rememberLazyListState()
    val favRowState = rememberLazyListState()
    val refreshedDetail = detailContact?.let { cur ->
        contacts.firstOrNull { it.id == cur.id } ?: cur
    }
    val refreshedEditing = editingContact?.let { cur ->
        contacts.firstOrNull { it.id == cur.id } ?: cur
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeaderRow(
            title = "CONTACTS DIRECTORY",
            subtitle = "${contacts.size} profiles • Calling Cards & Google Sync",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onPinContactWidget,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pin_contact_widget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToHomeScreen,
                            contentDescription = "Pin contact widget to home screen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showGoogleSyncSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("google_account_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google account sync settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onSyncDeviceContacts,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sync_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.action_sync_contacts),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_contact_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add contact", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text("Search name, number, email, or T9...", style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search contacts")
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contacts_search_input")
        )

        if (favorites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "STARRED GLYPHS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                state = favRowState,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = favorites,
                    key = { "fav_${it.id}" },
                    contentType = { "fav_chip" }
                ) { fav ->
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { detailContact = fav }
                            .testTag("favorite_chip_${fav.id}"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContactAvatarCircle(
                                name = fav.name,
                                photoUri = fav.photoUri,
                                size = 30.dp,
                                highlighted = true
                            )
                            Text(
                                text = fav.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            if (contacts.isEmpty()) {
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO CONTACTS IN MATRIX",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap NEW to create a contact, Sync to pull Google/device contacts, or tap the Google Account icon to add/remove synced accounts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = contacts,
                    key = { it.id },
                    contentType = { "contact_row" }
                ) { contact ->
                    val patternName = patternNameById[contact.vibrationPatternId] ?: "GLYPH PULSE"
                    val extraCount = remember(contact.additionalNumbersJson) {
                        if (contact.additionalNumbersJson.length <= 2) 0
                        else ContactNumbersCodec.decode(contact.additionalNumbersJson).size
                    }
                    NothingSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_card_${contact.id}"),
                        highlighted = contact.callingCardUri != null || contact.videoBackdropUri != null,
                        onClick = { detailContact = contact }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                ContactAvatarCircle(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 46.dp,
                                    highlighted = contact.isFavorite
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (contact.callingCardUri != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Badge,
                                                contentDescription = "Calling Card attached",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        if (contact.videoBackdropUri != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = "Video backdrop attached",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        if (contact.isBlocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "Blocked",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (extraCount > 0) {
                                            "${contact.phoneNumber}  (+$extraCount more)"
                                        } else {
                                            contact.phoneNumber
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (contact.accountName.isNotBlank()) {
                                            "${contact.accountName} • $patternName"
                                        } else {
                                            "VIBRATION: $patternName"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavorite(contact) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Toggle favorite",
                                        tint = if (contact.isFavorite) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onCallContact(contact) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("contact_call_btn_${contact.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${contact.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Contacts-Style Detail Sheet (Calling Card + Message, WhatsApp, Gmail, Call)
    if (refreshedDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { detailContact = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GoogleStyleContactDetailSheet(
                contact = refreshedDetail,
                showQuickActions = showQuickActions,
                onCallNumber = { numberToDial ->
                    detailContact = null
                    onCallNumber(numberToDial, refreshedDetail)
                },
                onSetDefaultNumber = { entry ->
                    onSetDefaultNumber(refreshedDetail, entry)
                },
                onSms = { numberForSms ->
                    val uri = Uri.parse("smsto:$numberForSms")
                    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                },
                onWhatsApp = { numberForWa ->
                    val digits = T9Utils.normalizePhone(numberForWa).filter { it.isDigit() }
                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
                    runCatching { context.startActivity(waIntent) }
                },
                onGmail = {
                    val emailTarget = refreshedDetail.email.trim()
                    val mailUri = Uri.parse("mailto:$emailTarget")
                    val mailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Hello ${refreshedDetail.name}")
                    }
                    runCatching { context.startActivity(mailIntent) }
                },
                onPickPhoto = {
                    mediaTargetContact = refreshedDetail
                    mediaTargetKind = ContactMediaTarget.PHOTO
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickBackdropPhoto = {
                    mediaTargetContact = refreshedDetail
                    mediaTargetKind = ContactMediaTarget.BACKDROP_PHOTO
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickCallingCard = {
                    mediaTargetContact = refreshedDetail
                    mediaTargetKind = ContactMediaTarget.CALLING_CARD
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickVideo = {
                    mediaTargetContact = refreshedDetail
                    mediaTargetKind = ContactMediaTarget.VIDEO_BACKDROP
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onSelectBackdropMode = { mode ->
                    onAssignBackdropMode(refreshedDetail, mode)
                },
                onEdit = {
                    editingContact = refreshedDetail
                    detailContact = null
                }
            )
        }
    }

    // Google Account Sync Add/Remove Sheet
    if (showGoogleSyncSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGoogleSyncSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GoogleAccountSyncSheet(
                googleSyncEnabled = googleSyncEnabled,
                discoveredAccounts = discoveredGoogleAccounts,
                syncedAccounts = syncedGoogleAccounts,
                onToggleSync = onToggleGoogleSync,
                onAddAccount = onAddGoogleAccount,
                onRemoveAccount = onRemoveGoogleAccount,
                onSyncNow = onSyncDeviceContacts
            )
        }
    }

    // Create / Edit Contact Sheet
    if (showCreateDialog || refreshedEditing != null) {
        val current = refreshedEditing
        ModalBottomSheet(
            onDismissRequest = {
                showCreateDialog = false
                editingContact = null
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ContactEditorSheetContent(
                initialContact = current,
                vibrationPatterns = vibrationPatterns,
                newContactStorageMode = newContactStorageMode,
                targetGoogleContactAccount = targetGoogleContactAccount,
                onPickVideo = { target ->
                    mediaTargetContact = target
                    mediaTargetKind = ContactMediaTarget.VIDEO_BACKDROP
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onPickPhoto = { target ->
                    mediaTargetContact = target
                    mediaTargetKind = ContactMediaTarget.PHOTO
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickBackdropPhoto = { target ->
                    mediaTargetContact = target
                    mediaTargetKind = ContactMediaTarget.BACKDROP_PHOTO
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickCallingCard = { target ->
                    mediaTargetContact = target
                    mediaTargetKind = ContactMediaTarget.CALLING_CARD
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSelectBackdropMode = { target, mode ->
                    onAssignBackdropMode(target, mode)
                },
                onClearVideo = { target -> onAssignVideoBackdrop(target, null) },
                onToggleBlocked = { target -> onToggleBlocked(target) },
                onAssignVibration = { target, patternId -> onAssignVibrationPattern(target, patternId) },
                onDelete = { id ->
                    onDeleteContact(id)
                    editingContact = null
                },
                onSave = { name, phone, company, email, notes, isFav, vibId, extraJson ->
                    onSaveContact(
                        current?.id,
                        name,
                        phone,
                        company,
                        email,
                        notes,
                        isFav,
                        current?.photoUri,
                        current?.callingCardUri,
                        current?.videoBackdropUri,
                        vibId,
                        extraJson
                    )
                    showCreateDialog = false
                    editingContact = null
                }
            )
        }
    }
}

@Composable
private fun GoogleStyleContactDetailSheet(
    contact: ContactEntity,
    showQuickActions: Boolean,
    onCallNumber: (String) -> Unit,
    onSetDefaultNumber: (ContactPhoneNumber) -> Unit,
    onSms: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onGmail: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickBackdropPhoto: () -> Unit,
    onPickCallingCard: () -> Unit,
    onPickVideo: () -> Unit,
    onSelectBackdropMode: (CallerBackdropMode) -> Unit,
    onEdit: () -> Unit
) {
    val activeBackdrop = effectiveContactBackdropMode(contact)
    val allNumbers = remember(contact.phoneNumber, contact.additionalNumbersJson) {
        contact.allPhoneNumbers()
    }
    var selectedNumber by remember(contact.id, contact.phoneNumber) {
        mutableStateOf(contact.phoneNumber)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calling Card / Gallery Photo Hero Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val heroUri = when (activeBackdrop) {
                CallerBackdropMode.CALLING_CARD -> contact.callingCardUri
                CallerBackdropMode.PHOTO -> contact.backdropPhotoUri ?: contact.photoUri
                else -> contact.callingCardUri ?: contact.backdropPhotoUri
            }
            if (!heroUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(heroUri),
                    contentDescription = "In-call backdrop poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.86f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatarCircle(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    size = 60.dp,
                    highlighted = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedNumber,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (contact.company.isNotBlank()) {
                        Text(
                            text = contact.company,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Google Contacts-style Direct Action Bar: Call + Toggleable SMS, WhatsApp, Gmail
        if (showQuickActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactQuickActionOrb(
                    icon = Icons.Default.Call,
                    label = "CALL",
                    primary = true,
                    onClick = { onCallNumber(selectedNumber) },
                    testTag = "contact_detail_call"
                )
                ContactQuickActionOrb(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = "SMS",
                    primary = false,
                    onClick = { onSms(selectedNumber) },
                    testTag = "contact_detail_sms"
                )
                ContactQuickActionOrb(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "WHATSAPP",
                    primary = false,
                    onClick = { onWhatsApp(selectedNumber) },
                    testTag = "contact_detail_whatsapp"
                )
                ContactQuickActionOrb(
                    icon = Icons.Default.Email,
                    label = "GMAIL",
                    primary = false,
                    onClick = onGmail,
                    testTag = "contact_detail_gmail"
                )
            }
        } else {
            Button(
                onClick = { onCallNumber(selectedNumber) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("contact_detail_call")
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALL $selectedNumber", style = MaterialTheme.typography.labelLarge)
            }
        }

        // All Phone Numbers for this person (with Set Default & 1-tap Call)
        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (allNumbers.size > 1) "PHONE NUMBERS (${allNumbers.size})" else "PHONE NUMBER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                allNumbers.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(color = LocalDotDialStyle.current.hairlineBorder)
                    }
                    val isSelected = selectedNumber == entry.number
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedNumber = entry.number }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("contact_detail_number_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.number,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (entry.isDefault) "${entry.label} • Default" else entry.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!entry.isDefault) {
                                TextButton(onClick = { onSetDefaultNumber(entry) }) {
                                    Text("DEFAULT", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(
                                onClick = { onCallNumber(entry.number) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call ${entry.number}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Exclusive Per-Contact In-Call Backdrop Studio + Avatar Photo
        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IN-CALL BACKDROP (1 ACTIVE AT A TIME)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = onPickPhoto,
                        modifier = Modifier.testTag("pick_contact_photo_button")
                    ) {
                        Text("AVATAR PHOTO", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DotMatrixTagChip(
                        label = if (activeBackdrop == CallerBackdropMode.VIDEO) "VIDEO • ON" else "VIDEO • OFF",
                        active = activeBackdrop == CallerBackdropMode.VIDEO,
                        onClick = {
                            if (activeBackdrop == CallerBackdropMode.VIDEO) {
                                onSelectBackdropMode(CallerBackdropMode.NONE)
                            } else if (!contact.videoBackdropUri.isNullOrBlank()) {
                                onSelectBackdropMode(CallerBackdropMode.VIDEO)
                            } else {
                                onPickVideo()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pick_contact_video_button"
                    )
                    DotMatrixTagChip(
                        label = if (activeBackdrop == CallerBackdropMode.PHOTO) "PHOTO • ON" else "PHOTO • OFF",
                        active = activeBackdrop == CallerBackdropMode.PHOTO,
                        onClick = {
                            if (activeBackdrop == CallerBackdropMode.PHOTO) {
                                onSelectBackdropMode(CallerBackdropMode.NONE)
                            } else if (!(contact.backdropPhotoUri ?: contact.photoUri).isNullOrBlank()) {
                                onSelectBackdropMode(CallerBackdropMode.PHOTO)
                            } else {
                                onPickBackdropPhoto()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pick_contact_backdrop_photo_button"
                    )
                    DotMatrixTagChip(
                        label = if (activeBackdrop == CallerBackdropMode.CALLING_CARD) "CARD • ON" else "CARD • OFF",
                        active = activeBackdrop == CallerBackdropMode.CALLING_CARD,
                        onClick = {
                            if (activeBackdrop == CallerBackdropMode.CALLING_CARD) {
                                onSelectBackdropMode(CallerBackdropMode.NONE)
                            } else if (!contact.callingCardUri.isNullOrBlank()) {
                                onSelectBackdropMode(CallerBackdropMode.CALLING_CARD)
                            } else {
                                onPickCallingCard()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pick_calling_card_button"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            when (activeBackdrop) {
                                CallerBackdropMode.PHOTO -> onPickBackdropPhoto()
                                CallerBackdropMode.CALLING_CARD -> onPickCallingCard()
                                else -> onPickVideo()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (activeBackdrop) {
                                CallerBackdropMode.PHOTO -> "CHANGE IN-CALL PHOTO"
                                CallerBackdropMode.CALLING_CARD -> "CHANGE CALLING CARD"
                                else -> "PICK / CHANGE VIDEO"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (activeBackdrop != CallerBackdropMode.NONE) {
                        OutlinedButton(
                            onClick = { onSelectBackdropMode(CallerBackdropMode.NONE) },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("OFF", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (contact.email.isNotBlank() || contact.notes.isNotBlank()) {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (contact.email.isNotBlank()) {
                        Text(
                            text = "GMAIL / EMAIL: ${contact.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (contact.notes.isNotBlank()) {
                        Text(
                            text = "NOTES: ${contact.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onEdit,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("contact_detail_edit_button")
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("EDIT PROFILE & GLYPH", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun ContactQuickActionOrb(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(52.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag),
            shape = CircleShape,
            color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, LocalDotDialStyle.current.hairlineBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (primary) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GoogleAccountSyncSheet(
    googleSyncEnabled: Boolean,
    discoveredAccounts: List<String>,
    syncedAccounts: Set<String>,
    onToggleSync: (Boolean) -> Unit,
    onAddAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onSyncNow: () -> Unit
) {
    var manualAccountEmail by remember { mutableStateOf("") }
    val allKnown = remember(discoveredAccounts, syncedAccounts) {
        (discoveredAccounts + syncedAccounts).distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "GOOGLE ACCOUNT CONTACT SYNC",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sync Google Contacts",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val hapticPulse = LocalUiHapticPulse.current
            Switch(
                checked = googleSyncEnabled,
                onCheckedChange = {
                    hapticPulse()
                    onToggleSync(it)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualAccountEmail,
                onValueChange = { manualAccountEmail = it },
                placeholder = { Text("Add Google email (e.g. name@gmail.com)") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("google_account_add_input")
            )
            Button(
                onClick = {
                    onAddAccount(manualAccountEmail)
                    manualAccountEmail = ""
                },
                enabled = manualAccountEmail.contains("@"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("google_account_add_button")
            ) {
                Text("ADD", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (allKnown.isEmpty()) {
            Text(
                text = "No specific Google accounts pinned yet. Add an email above or tap Sync Now to pull all device Google contacts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            allKnown.forEach { email ->
                val isIncluded = syncedAccounts.isEmpty() || syncedAccounts.contains(email)
                NothingSurfaceCard(modifier = Modifier.fillMaxWidth(), highlighted = isIncluded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isIncluded) "Synced with Dot Dial" else "Removed from sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isIncluded) {
                            IconButton(
                                onClick = { onRemoveAccount(email) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Google account",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            TextButton(onClick = { onAddAccount(email) }) {
                                Text("ADD")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onSyncNow,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_sync_now_button")
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SYNC GOOGLE CONTACTS NOW", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun ContactEditorSheetContent(
    initialContact: ContactEntity?,
    vibrationPatterns: List<VibrationPatternEntity>,
    newContactStorageMode: NewContactStorageMode,
    targetGoogleContactAccount: String,
    onPickVideo: (ContactEntity) -> Unit,
    onPickPhoto: (ContactEntity) -> Unit,
    onPickBackdropPhoto: (ContactEntity) -> Unit,
    onPickCallingCard: (ContactEntity) -> Unit,
    onSelectBackdropMode: (ContactEntity, CallerBackdropMode) -> Unit,
    onClearVideo: (ContactEntity) -> Unit,
    onToggleBlocked: (ContactEntity) -> Unit,
    onAssignVibration: (ContactEntity, Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSave: (String, String, String, String, String, Boolean, Long, String) -> Unit
) {
    var name by remember(initialContact) { mutableStateOf(initialContact?.name.orEmpty()) }
    var phone by remember(initialContact) { mutableStateOf(initialContact?.phoneNumber.orEmpty()) }
    val extraNumbers = remember(initialContact) {
        mutableStateListOf<ContactPhoneNumber>().apply {
            initialContact?.additionalNumbersJson?.let { addAll(ContactNumbersCodec.decode(it)) }
        }
    }
    var company by remember(initialContact) { mutableStateOf(initialContact?.company.orEmpty()) }
    var email by remember(initialContact) { mutableStateOf(initialContact?.email.orEmpty()) }
    var notes by remember(initialContact) { mutableStateOf(initialContact?.notes.orEmpty()) }
    var isFav by remember(initialContact) { mutableStateOf(initialContact?.isFavorite ?: false) }
    var selectedVibId by remember(initialContact) {
        mutableLongStateOf(initialContact?.vibrationPatternId ?: 1L)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialContact == null) "NEW CONTACT PROFILE" else "EDIT CONTACT PROFILE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (initialContact != null) {
                IconButton(
                    onClick = { onDelete(initialContact.id) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (initialContact == null) {
            val savingToGoogle = newContactStorageMode == NewContactStorageMode.GOOGLE_ACCOUNT &&
                targetGoogleContactAccount.isNotBlank()
            val destinationLabel = when {
                savingToGoogle -> "SAVING TO GOOGLE • $targetGoogleContactAccount"
                newContactStorageMode == NewContactStorageMode.GOOGLE_ACCOUNT ->
                    "NO GOOGLE ACCOUNT SIGNED IN • SAVING TO DEVICE"
                else -> "SAVING TO DEVICE STORAGE"
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (savingToGoogle) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(1.dp, LocalDotDialStyle.current.hairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_contact_storage_destination_pill")
            ) {
                Text(
                    text = destinationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (savingToGoogle) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Default Phone Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
        )
        extraNumbers.forEachIndexed { index, extra ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = extra.number,
                    onValueChange = { updated ->
                        extraNumbers[index] = extra.copy(number = updated)
                    },
                    label = { Text("${extra.label} Number") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { extraNumbers.removeAt(index) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove extra number",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        TextButton(
            onClick = { extraNumbers.add(ContactPhoneNumber("", "Other")) }
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ADD ANOTHER NUMBER", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Gmail / Email (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("contact_email_input")
        )
        OutlinedTextField(
            value = company,
            onValueChange = { company = it },
            label = { Text("Label / Company (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Starred Favorite",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val editorHapticPulse = LocalUiHapticPulse.current
            Switch(
                checked = isFav,
                onCheckedChange = {
                    editorHapticPulse()
                    isFav = it
                }
            )
        }

        if (initialContact != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Block Caller (Spam Shield)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val blockHapticPulse = LocalUiHapticPulse.current
                Switch(
                    checked = initialContact.isBlocked,
                    onCheckedChange = {
                        blockHapticPulse()
                        onToggleBlocked(initialContact)
                    }
                )
            }

            val editorBackdrop = effectiveContactBackdropMode(initialContact)
            Text(
                text = "IN-CALL BACKDROP MODE (1 ACTIVE • OTHERS OFF)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DotMatrixTagChip(
                    label = if (editorBackdrop == CallerBackdropMode.VIDEO) "VIDEO • ON" else "VIDEO • OFF",
                    active = editorBackdrop == CallerBackdropMode.VIDEO,
                    onClick = {
                        if (editorBackdrop == CallerBackdropMode.VIDEO) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.NONE)
                        } else if (!initialContact.videoBackdropUri.isNullOrBlank()) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.VIDEO)
                        } else {
                            onPickVideo(initialContact)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                DotMatrixTagChip(
                    label = if (editorBackdrop == CallerBackdropMode.PHOTO) "PHOTO • ON" else "PHOTO • OFF",
                    active = editorBackdrop == CallerBackdropMode.PHOTO,
                    onClick = {
                        if (editorBackdrop == CallerBackdropMode.PHOTO) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.NONE)
                        } else if (!(initialContact.backdropPhotoUri ?: initialContact.photoUri).isNullOrBlank()) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.PHOTO)
                        } else {
                            onPickBackdropPhoto(initialContact)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                DotMatrixTagChip(
                    label = if (editorBackdrop == CallerBackdropMode.CALLING_CARD) "CARD • ON" else "CARD • OFF",
                    active = editorBackdrop == CallerBackdropMode.CALLING_CARD,
                    onClick = {
                        if (editorBackdrop == CallerBackdropMode.CALLING_CARD) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.NONE)
                        } else if (!initialContact.callingCardUri.isNullOrBlank()) {
                            onSelectBackdropMode(initialContact, CallerBackdropMode.CALLING_CARD)
                        } else {
                            onPickCallingCard(initialContact)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onPickPhoto(initialContact) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("AVATAR PHOTO", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        when (editorBackdrop) {
                            CallerBackdropMode.PHOTO -> onPickBackdropPhoto(initialContact)
                            CallerBackdropMode.CALLING_CARD -> onPickCallingCard(initialContact)
                            else -> onPickVideo(initialContact)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (editorBackdrop) {
                            CallerBackdropMode.PHOTO -> "PICK PHOTO"
                            CallerBackdropMode.CALLING_CARD -> "PICK CARD"
                            else -> "PICK VIDEO"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (initialContact.videoBackdropUri != null && editorBackdrop == CallerBackdropMode.VIDEO) {
                TextButton(onClick = { onClearVideo(initialContact) }) {
                    Text("CLEAR VIDEO BACKDROP")
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = "Vibration glyph",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "INCOMING GLYPH VIBRATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vibrationPatterns, key = { it.id }, contentType = { "vibration_pattern" }) { pattern ->
                DotMatrixTagChip(
                    label = pattern.name,
                    active = selectedVibId == pattern.id,
                    onClick = {
                        selectedVibId = pattern.id
                        if (initialContact != null) {
                            onAssignVibration(initialContact, pattern.id)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                onSave(
                    name,
                    phone,
                    company,
                    email,
                    notes,
                    isFav,
                    selectedVibId,
                    ContactNumbersCodec.encode(extraNumbers)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_contact_button")
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE CONTACT", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
