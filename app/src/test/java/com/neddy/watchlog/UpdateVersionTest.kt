package com.neddy.watchlog

import com.neddy.watchlog.data.update.UpdateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun newerTagIsDetected() {
        assertTrue(UpdateRepository.compareVersions("v1.3", "1.2") > 0)
        assertTrue(UpdateRepository.compareVersions("v1.10", "1.9") > 0)
        assertTrue(UpdateRepository.compareVersions("2.0", "1.9.9") > 0)
    }

    @Test
    fun sameOrOlderTagIsNotAnUpdate() {
        assertEquals(0, UpdateRepository.compareVersions("v1.2", "1.2"))
        assertEquals(0, UpdateRepository.compareVersions("1.2.0", "1.2"))
        assertTrue(UpdateRepository.compareVersions("v1.1", "1.2") < 0)
    }

    @Test
    fun versionIsNormalizedWithoutPrefix() {
        assertEquals("1.3", UpdateRepository.normalizeVersion(" v1.3 "))
        assertEquals("1.3", UpdateRepository.normalizeVersion("V1.3"))
    }
}
