package com.taksolutions.uasready.release

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * Gradle reads version.properties with java.util.Properties. A UTF-8 BOM turns the first key into
 * "﻿VERSION_CODE"-as-Latin-1, so the version code was silently lost (every build was versionCode 1).
 */
class VersionPropertiesTest {

    private val file = File("..", "version.properties")

    @Test
    fun fileHasNoByteOrderMark() {
        val head = file.readBytes().take(3).map { it.toInt() and 0xFF }
        assertNotEquals("version.properties must be saved without a UTF-8 BOM", listOf(0xEF, 0xBB, 0xBF), head)
    }

    @Test
    fun versionCodeAndNameAreReadableTheWayGradleReadsThem() {
        val props = Properties().apply { file.inputStream().use { load(it) } }
        val code = props.getProperty("VERSION_CODE")?.trim()?.toIntOrNull()
        assertNotNull("VERSION_CODE must be present and numeric", code)
        assertTrue("VERSION_CODE must be positive", code!! > 0)
        assertTrue("VERSION_NAME must be X.Y.Z", Regex("""\d+\.\d+\.\d+""").matches(props.getProperty("VERSION_NAME")?.trim().orEmpty()))
    }
}
