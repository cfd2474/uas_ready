package com.taksolutions.uasready.release

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Properties

/** CLAUDE.md §9: the newest release-notes section must be the shipping version. */
class ReleaseNotesTest {

    // Unit tests run with the app module as working directory.
    private val root = File("..")
    private val notes = File(root, "docs/RELEASE_NOTES.md").readText()
    private val versionName = Properties().apply { File(root, "version.properties").inputStream().use { load(it) } }
        .getProperty("VERSION_NAME").trim()

    private val headings = Regex("""^## v(\d+\.\d+\.\d+) — (\d{4}-\d{2}-\d{2})\s*$""", RegexOption.MULTILINE)

    @Test
    fun newestSectionIsTheVersionInVersionProperties() {
        val newest = headings.find(notes)
        assertNotNull("docs/RELEASE_NOTES.md has no '## vX.Y.Z — YYYY-MM-DD' section", newest)
        assertEquals("Newest release-notes section must match version.properties VERSION_NAME", versionName, newest!!.groupValues[1])
    }

    @Test
    fun playWhatsNewFitsTheConsoleLimit() {
        val newestSection = notes.substring(headings.find(notes)!!.range.first).let { s ->
            headings.findAll(s).drop(1).firstOrNull()?.let { s.substring(0, it.range.first) } ?: s
        }
        val whatsNew = Regex("```\n(.*?)```", RegexOption.DOT_MATCHES_ALL).find(newestSection)?.groupValues?.get(1)
        assertNotNull("Newest section needs a fenced Google Play \"What's new\" block", whatsNew)
        assertTrue("Play \"What's new\" is ${whatsNew!!.length} chars; limit 500", whatsNew.length <= 500)
    }
}
