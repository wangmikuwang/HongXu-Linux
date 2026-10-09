package io.wenyou.textquest

import io.wenyou.textquest.data.UpdatePolicy
import io.wenyou.textquest.data.isNewerVersion
import io.wenyou.textquest.data.parseUpdatePolicy
import io.wenyou.textquest.data.requiresAppUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The Linux edition's own version line starts at 1.0.0; Android's minimum must never lock it out. */
class LinuxUpdatePolicyTest {
    @Test fun firstReleaseIsNotBlocked() {
        assertFalse(requiresAppUpdate(1, "1.0.0", UpdatePolicy()))
        assertFalse(requiresAppUpdate(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, parseUpdatePolicy(File("update-policy.json").readText())))
    }

    @Test fun policyStillBlocksOlderVersions() {
        assertTrue(requiresAppUpdate(1, "1.0.0", UpdatePolicy(2, "1.1.0")))
        assertEquals(UpdatePolicy(3, "1.2.0"), parseUpdatePolicy("""{"minimumVersionCode":3,"minimumVersion":"1.2.0"}"""))
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
    }
}
