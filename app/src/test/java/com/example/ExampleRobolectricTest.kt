package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.example.data.local.ContactEntity
import com.example.data.preferences.AiAssistantTriggerMode
import com.example.data.preferences.CallerBackdropMode
import com.example.data.preferences.DialerSettings
import com.example.telecom.CallSessionBridge

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Dot Dial", appName)
  }

  @Test
  fun `ai assistant trigger mode defaults to busy mode only`() {
    assertEquals(AiAssistantTriggerMode.BUSY_MODE_ONLY, DialerSettings().aiAssistantTriggerMode)
    assertEquals(AiAssistantTriggerMode.BUSY_MODE_ONLY, AiAssistantTriggerMode.fromId("unknown"))
  }

  @Test
  fun `resolveBackdropSpec keeps only one backdrop mode active`() {
    val base = ContactEntity(
      id = 1L,
      name = "Alex Mercer",
      phoneNumber = "+15551234567",
      normalizedNumber = "15551234567",
      t9Key = "2539",
      photoUri = "content://avatar/1",
      videoBackdropUri = "content://video/1",
      backdropPhotoUri = "content://photo/1",
      callingCardUri = "content://card/1",
      callerBackdropMode = CallerBackdropMode.PHOTO.id
    )
    val photoResolved = CallSessionBridge.resolveBackdropSpec(base, DialerSettings())
    assertEquals(CallerBackdropMode.PHOTO, photoResolved.mode)
    assertEquals("content://photo/1", photoResolved.photoUri)
    assertEquals(null, photoResolved.videoUri)

    val videoResolved = CallSessionBridge.resolveBackdropSpec(
      base.copy(callerBackdropMode = CallerBackdropMode.VIDEO.id),
      DialerSettings()
    )
    assertEquals(CallerBackdropMode.VIDEO, videoResolved.mode)
    assertEquals("content://video/1", videoResolved.videoUri)
    assertEquals(null, videoResolved.photoUri)

    val cardResolved = CallSessionBridge.resolveBackdropSpec(
      base.copy(callerBackdropMode = CallerBackdropMode.CALLING_CARD.id),
      DialerSettings()
    )
    assertEquals(CallerBackdropMode.CALLING_CARD, cardResolved.mode)
    assertEquals("content://card/1", cardResolved.callingCardUri)
    assertEquals(null, cardResolved.photoUri)
    assertEquals(null, cardResolved.videoUri)
  }

  @Test
  fun `first launch google sign in prompt flag stays prompted after dismiss or sign out`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.preferences.DialerPreferences(context)
    assertEquals(false, prefs.currentSettingsOnce().hasPromptedGoogleSignIn)

    prefs.setHasPromptedGoogleSignIn(true)
    assertEquals(true, prefs.currentSettingsOnce().hasPromptedGoogleSignIn)

    prefs.setSignedInGoogleProfile("user@example.com", "Alex", "")
    prefs.setSignedInGoogleProfile("", "", "")
    assertEquals(true, prefs.currentSettingsOnce().hasPromptedGoogleSignIn)
  }

  @Test
  fun `ui vibration defaults to true and toggles cleanly with json backup`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.preferences.DialerPreferences(context)
    assertEquals(true, DialerSettings().uiVibrationEnabled)
    assertEquals(true, prefs.currentSettingsOnce().uiVibrationEnabled)

    prefs.setUiVibrationEnabled(false)
    assertEquals(false, prefs.currentSettingsOnce().uiVibrationEnabled)

    val json = prefs.encodeSettingsJson(prefs.currentSettingsOnce().copy(uiVibrationEnabled = true))
    prefs.applySettingsJson(json)
    assertEquals(true, prefs.currentSettingsOnce().uiVibrationEnabled)
  }

  @Test
  fun `system bar icon polarity follows active theme luminance independently of system mode`() {
    val lightThemeResult = com.example.ui.theme.resolveSystemBarsUseDarkIcons(
      settings = DialerSettings(uiBackgroundMode = com.example.data.preferences.UiBackgroundMode.DOT_MATRIX),
      themeBackground = androidx.compose.ui.graphics.Color.White,
      isThemeDark = false
    )
    assertEquals(true to true, lightThemeResult)

    val darkThemeResult = com.example.ui.theme.resolveSystemBarsUseDarkIcons(
      settings = DialerSettings(uiBackgroundMode = com.example.data.preferences.UiBackgroundMode.DOT_MATRIX),
      themeBackground = androidx.compose.ui.graphics.Color(0xFF0A0A0E),
      isThemeDark = true
    )
    assertEquals(false to false, darkThemeResult)

    val splitGradientResult = com.example.ui.theme.resolveSystemBarsUseDarkIcons(
      settings = DialerSettings(
        uiBackgroundMode = com.example.data.preferences.UiBackgroundMode.COLOR_PLATE,
        colorPlateStyle = com.example.data.preferences.ColorPlateStyle.GRADIENT,
        gradientStartHex = 0xFFFFFFFFL,
        gradientEndHex = 0xFF050508L
      ),
      themeBackground = androidx.compose.ui.graphics.Color.Black,
      isThemeDark = true
    )
    assertEquals(true to false, splitGradientResult)
  }
}
