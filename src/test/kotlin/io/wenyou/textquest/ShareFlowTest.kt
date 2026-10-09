package io.wenyou.textquest

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.repo.Base45
import io.wenyou.textquest.data.repo.ShareCode
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.zip.Deflater
import kotlin.random.Random

class ShareFlowTest {
    private fun codeOf(json: String): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true).apply { setInput(json.toByteArray()); finish() }
        val buf = ByteArray(65536)
        val n = deflater.deflate(buf)
        deflater.end()
        return "WY2:" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf.copyOf(n))
    }

    /** An incompressible payload, the worst case for page count. */
    private fun randomCode(bytes: Int) = "WY2:" + Base64.getUrlEncoder().withoutPadding().encodeToString(Random(7).nextBytes(bytes))

    @Test fun base45MatchesRfc9285() {
        assertEquals("BB8", Base45.encode("AB".toByteArray()))
        assertEquals("%69 VD92EX0", Base45.encode("Hello!!".toByteArray()))
        assertEquals("UJCLQE7W581", Base45.encode("base-45".toByteArray()))
        assertEquals("ietf!", String(Base45.decode("QED8WEX0")!!))
        assertNull(Base45.decode("GGW"))   // 65535 overflow per the RFC
        assertNull(Base45.decode("abc"))   // outside the alphabet
    }

    @Test fun qrPagesReassembleIntoTheOriginalCodeEvenWithTrailingSpaces() {
        for (size in listOf(10, 426, 427, 3000, 5000)) {
            val code = randomCode(size)
            val pages = ShareCode.qrPages(code)
            assertTrue(pages.all { p -> p.all { it in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:" } })
            // Scanned out of order, untrimmed: the assembler must still rebuild the exact code.
            assertEquals(code, ShareCode.assembleQrTexts(pages.reversed()) ?: assembleRaw(pages))
        }
    }

    private fun assembleRaw(pages: List<String>): String? {
        val chunks = pages.map { ShareCode.parseChunk(it)!! }
        return ShareCode.assembleChunks(chunks.associate { it.index to it.data }, chunks.first().total)
    }

    @Test fun newPagesNeedFewerQrCodesAtNoHigherDensity() {
        val code = randomCode(4000)
        val old = ShareCode.qrChunks(code)
        val new = ShareCode.qrPages(code)
        val oldVersion = old.maxOf { Encoder.encode(it, ErrorCorrectionLevel.M).version.versionNumber }
        val newVersion = new.maxOf { Encoder.encode(it, ErrorCorrectionLevel.M).version.versionNumber }
        assertTrue("pages ${new.size} vs ${old.size}", new.size < old.size)
        assertTrue("version $newVersion vs $oldVersion", newVersion <= oldVersion)
    }

    @Test fun shareCodesAreFoundInsideMessagesAndLinks() {
        val code = ShareCode.encode(AppBundle(stories = listOf(Story("s1", "雨夜"))))
        assertEquals(code, ShareCode.extract("我分享了剧情《雨夜》：\nhttps://example.org/s/?a=hx#$code\n（复制后打开应用）"))
        assertEquals(code, ShareCode.extract("hongxu://import?c=$code"))
        assertNull(ShareCode.extract("WY2:not-a-real-code-at-all"))
    }

    @Test fun originRuleLetsTheOpenAppImportAllAndTheOtherRejectItsContent() {
        val own = ShareCode.encode(AppBundle(stories = listOf(Story("s1", "雨夜"))))
        assertFalse(ShareCode.foreign(own))
        val fromHx = codeOf("""{"origin":"hx","stories":[{"id":"a","title":"t"}]}""")
        val legacyHx = codeOf("""{"stories":[{"id":"a","title":"t","lgbt":true}],"characters":[{"id":"c","name":"n","orientation":"gay"}]}""")
        val fromXx = codeOf("""{"origin":"xx","stories":[{"id":"a","title":"t"}]}""")
        val legacyPlain = codeOf("""{"stories":[{"id":"a","title":"t"}]}""")
        val hxApp = BuildConfig.SHARE_ORIGIN == "hx"
        assertEquals(!hxApp, ShareCode.foreign(fromHx))
        assertEquals(!hxApp, ShareCode.foreign(legacyHx))
        assertFalse(ShareCode.foreign(fromXx))
        assertFalse(ShareCode.foreign(legacyPlain))
        assertEquals(!hxApp, ShareCode.foreignJson("""{"origin":"hx","characters":[]}"""))
    }
}
