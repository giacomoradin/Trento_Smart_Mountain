package it.trentosmartmountain.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test di [downsampleByIndex], il campionatore usato per la route signature
 * e il profilo altimetrico inviati al feed. Speculare ai test backend
 * (`backend/__tests__/utils/geoPolyline.test.js`): il client deve ridurre la
 * traccia esattamente come il server, primo/ultimo punto preservati.
 */
class GeoSamplingTest {

    @Test
    fun `ritorna null con meno di 2 punti`() {
        assertNull(downsampleByIndex(emptyList<Int>(), 10))
        assertNull(downsampleByIndex(listOf(42), 10))
    }

    @Test
    fun `ritorna l'input invariato se gia entro maxPoints`() {
        val pts = listOf(1, 2, 3)
        assertEquals(pts, downsampleByIndex(pts, 10))
        assertEquals(pts, downsampleByIndex(pts, 3))
    }

    @Test
    fun `campiona a esattamente maxPoints preservando primo e ultimo`() {
        val pts = (0 until 1000).toList()
        val out = downsampleByIndex(pts, 48)
        assertEquals(48, out!!.size)
        assertEquals(0, out.first())
        assertEquals(999, out.last())
    }

    @Test
    fun `caso limite di 2 punti`() {
        assertEquals(listOf(10, 20), downsampleByIndex(listOf(10, 20), 2))
    }

    @Test
    fun `e generico e funziona su tipi non numerici`() {
        val pts = listOf("a", "b", "c", "d", "e")
        val out = downsampleByIndex(pts, 3)
        assertEquals(3, out!!.size)
        assertEquals("a", out.first())
        assertEquals("e", out.last())
    }
}
