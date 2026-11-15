package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.ContactEntity
import com.example.data.local.DotDialDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DotDialContactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgets(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, DotDialContactWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                onUpdate(context, mgr, ids)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_CONTACTS"
        const val EXTRA_WIDGET_CALL_NOW = "extra_widget_call_now"
        const val EXTRA_OPEN_CONTACTS = "extra_open_contacts"

        fun refreshAll(context: Context) {
            val appCtx = context.applicationContext
            val mgr = AppWidgetManager.getInstance(appCtx)
            val ids = mgr.getAppWidgetIds(
                ComponentName(appCtx, DotDialContactWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                updateWidgets(appCtx, mgr, ids)
            }
        }

        fun isPinSupported(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val mgr = AppWidgetManager.getInstance(context)
            return mgr.isRequestPinAppWidgetSupported
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val mgr = AppWidgetManager.getInstance(context)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DotDialContactWidgetProvider::class.java)
            return mgr.requestPinAppWidget(provider, null, null)
        }

        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val contacts = runCatching {
                DotDialDatabase.getInstance(context)
                    .contactDao()
                    .getAllOnce()
                    .filter { !it.isBlocked }
                    .sortedWith(
                        compareByDescending<ContactEntity> { it.isFavorite }
                            .thenBy { it.name.lowercase() }
                    )
                    .take(4)
            }.getOrDefault(emptyList())

            appWidgetIds.forEach { widgetId ->
                val views = buildRemoteViews(context, contacts, widgetId)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            contacts: List<ContactEntity>,
            widgetId: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_contacts)

            val openContactsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_CONTACTS, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openContactsPending = PendingIntent.getActivity(
                context,
                9000 + widgetId,
                openContactsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open_contacts_btn, openContactsPending)
            views.setOnClickPendingIntent(R.id.widget_empty_state, openContactsPending)

            if (contacts.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_state, View.VISIBLE)
                views.setViewVisibility(R.id.widget_row_1, View.GONE)
                views.setViewVisibility(R.id.widget_row_2, View.GONE)
                return views
            }

            views.setViewVisibility(R.id.widget_empty_state, View.GONE)
            views.setViewVisibility(R.id.widget_row_1, View.VISIBLE)
            views.setViewVisibility(
                R.id.widget_row_2,
                if (contacts.size > 2) View.VISIBLE else View.GONE
            )

            val slotIds = listOf(
                WidgetSlotIds(
                    R.id.widget_slot_1,
                    R.id.widget_initial_1,
                    R.id.widget_avatar_1,
                    R.id.widget_name_1,
                    R.id.widget_number_1,
                    R.id.widget_call_1
                ),
                WidgetSlotIds(
                    R.id.widget_slot_2,
                    R.id.widget_initial_2,
                    R.id.widget_avatar_2,
                    R.id.widget_name_2,
                    R.id.widget_number_2,
                    R.id.widget_call_2
                ),
                WidgetSlotIds(
                    R.id.widget_slot_3,
                    R.id.widget_initial_3,
                    R.id.widget_avatar_3,
                    R.id.widget_name_3,
                    R.id.widget_number_3,
                    R.id.widget_call_3
                ),
                WidgetSlotIds(
                    R.id.widget_slot_4,
                    R.id.widget_initial_4,
                    R.id.widget_avatar_4,
                    R.id.widget_name_4,
                    R.id.widget_number_4,
                    R.id.widget_call_4
                )
            )

            slotIds.forEachIndexed { index, slot ->
                val contact = contacts.getOrNull(index)
                if (contact == null) {
                    views.setViewVisibility(slot.containerId, View.INVISIBLE)
                } else {
                    views.setViewVisibility(slot.containerId, View.VISIBLE)
                    views.setTextViewText(slot.nameId, contact.name)
                    views.setTextViewText(slot.numberId, contact.phoneNumber)
                    views.setTextViewText(
                        slot.initialId,
                        contact.name.trim().take(1).uppercase().ifEmpty { "#" }
                    )

                    val avatarBitmap = loadCompactCircularAvatar(context, contact.photoUri)
                    if (avatarBitmap != null) {
                        views.setImageViewBitmap(slot.avatarId, avatarBitmap)
                        views.setViewVisibility(slot.avatarId, View.VISIBLE)
                        views.setViewVisibility(slot.initialId, View.GONE)
                    } else {
                        views.setViewVisibility(slot.avatarId, View.GONE)
                        views.setViewVisibility(slot.initialId, View.VISIBLE)
                    }

                    val callIntent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_DIAL
                        data = Uri.fromParts("tel", contact.phoneNumber, null)
                        putExtra(EXTRA_WIDGET_CALL_NOW, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val callPending = PendingIntent.getActivity(
                        context,
                        10_000 + widgetId * 10 + index,
                        callIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(slot.containerId, callPending)
                    views.setOnClickPendingIntent(slot.callBtnId, callPending)
                }
            }

            return views
        }

        private fun loadCompactCircularAvatar(context: Context, uriString: String?): Bitmap? {
            if (uriString.isNullOrBlank()) return null
            val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
            val targetPx = 72
            return runCatching {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > targetPx * 2) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return null

                val scaled = Bitmap.createScaledBitmap(raw, targetPx, targetPx, true)
                if (scaled != raw) raw.recycle()

                val output = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = Rect(0, 0, targetPx, targetPx)
                val radius = targetPx / 2f
                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawCircle(radius, radius, radius, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, rect, rect, paint)
                scaled.recycle()
                output
            }.getOrNull()
        }

        private data class WidgetSlotIds(
            val containerId: Int,
            val initialId: Int,
            val avatarId: Int,
            val nameId: Int,
            val numberId: Int,
            val callBtnId: Int
        )
    }
}
