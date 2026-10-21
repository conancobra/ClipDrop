package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlatformType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClipDrop", appName)
    }

    @Test
    fun `platform detection works for supported services`() {
        assertEquals(
            PlatformType.YOUTUBE,
            PlatformType.fromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            PlatformType.TIKTOK,
            PlatformType.fromUrl("https://www.tiktok.com/@creator/video/123456789")
        )
        assertEquals(
            PlatformType.INSTAGRAM,
            PlatformType.fromUrl("https://www.instagram.com/reel/C8xyz123/")
        )
        assertEquals(
            PlatformType.FACEBOOK,
            PlatformType.fromUrl("https://www.facebook.com/watch/?v=987654321")
        )
        assertTrue(
            PlatformType.isValidVideoUrl("https://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test
    fun `extractUrlFromText correctly extracts URL from surrounding social share text`() {
        val sharedFromTikTok = "Schau dir dieses coole Video an! https://vm.tiktok.com/ZM1234567/ viel Spaß"
        val extracted = PlatformType.extractUrlFromText(sharedFromTikTok)
        assertEquals("https://vm.tiktok.com/ZM1234567/", extracted)
        assertEquals(PlatformType.TIKTOK, PlatformType.fromUrl(extracted))

        val sharedFromInsta = "Check this out https://www.instagram.com/reel/C8xyz123/"
        val extractedInsta = PlatformType.extractUrlFromText(sharedFromInsta)
        assertEquals("https://www.instagram.com/reel/C8xyz123/", extractedInsta)
        assertEquals(PlatformType.INSTAGRAM, PlatformType.fromUrl(extractedInsta))
    }
}
