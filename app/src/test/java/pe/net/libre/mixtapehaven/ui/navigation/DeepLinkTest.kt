package pe.net.libre.mixtapehaven.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {

    @Test
    fun `opens now playing from home`() {
        assertEquals(Routes.NOW_PLAYING, resolveDeepLink(Routes.NOW_PLAYING, Routes.HOME))
    }

    @Test
    fun `opens now playing from a nested screen`() {
        assertEquals(Routes.NOW_PLAYING, resolveDeepLink(Routes.NOW_PLAYING, Routes.SETTINGS))
    }

    @Test
    fun `ignores the request while signed out`() {
        assertNull(resolveDeepLink(Routes.NOW_PLAYING, Routes.LOGIN))
    }

    @Test
    fun `ignores the request when already on the target`() {
        assertNull(resolveDeepLink(Routes.NOW_PLAYING, Routes.NOW_PLAYING))
    }
}
