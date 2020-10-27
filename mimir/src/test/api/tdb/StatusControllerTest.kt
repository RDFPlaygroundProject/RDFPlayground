package api.tdb

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StatusControllerTest {

    @Test
    fun status() {
        // Only if you delete the resources/tdb directory or corrupt it should fail
        val controller = StatusController()
        val response = controller.status()
        assertEquals(200, response.statusCodeValue)
    }
}