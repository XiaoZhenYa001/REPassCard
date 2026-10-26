package com.example.passcard.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Base64
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RestoredIconRenderingTest {
    @Test fun restoredSvgWithALongXmlCommentRendersWithoutAnExtension() {
        val svg = "<?xml version=\"1.0\"?><!--" + "padding ".repeat(90) +
            "--><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\">" +
            "<rect width=\"20\" height=\"20\" fill=\"red\"/></svg>"
        withPrivateIcon(svg.toByteArray()) { context, reference ->
            val bitmap = PasswordIconStorage.decodeIconBitmap(context, reference, 64)
            assertNotNull(bitmap)
            assertEquals(64, bitmap!!.width)
            assertEquals(64, bitmap.height)
        }
    }

    @Test fun restoredRasterStillRendersWithoutAnExtension() {
        val png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==")
        withPrivateIcon(png) { context, reference ->
            val bitmap = PasswordIconStorage.decodeIconBitmap(context, reference, 64)
            assertNotNull(bitmap)
            assertEquals(64, bitmap!!.width)
            assertEquals(64, bitmap.height)
        }
    }

    @Test fun undecodableBytesUseTheExistingFallback() {
        withPrivateIcon("not an image".toByteArray()) { context, reference ->
            assertNull(PasswordIconStorage.decodeIconBitmap(context, reference, 64))
        }
    }

    private fun withPrivateIcon(bytes: ByteArray, check: (Context, String) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "restored-icon-${UUID.randomUUID()}.bin")
        try {
            file.writeBytes(bytes)
            check(context, file.toURI().toString())
        } finally {
            file.delete()
        }
    }
}
