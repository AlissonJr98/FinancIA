package com.financia.app.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Testes de regressão para validação e proteção de tamanho máximo de arquivo de extrato bancário.
 */
class BankStatementImporterTest {

    @Test
    fun testMaxFileSizeBytesIsFiveMegabytes() {
        assertEquals(5 * 1024 * 1024, BankStatementImporter.MAX_FILE_SIZE_BYTES)
    }

    @Test
    fun testReadLimitedStreamUnderLimitReturnsContent() {
        val sampleOfx = "<OFX><STMTTRN><TRNAMT>-45.90<MEMO>Supermercado</STMTTRN></OFX>"
        val stream = ByteArrayInputStream(sampleOfx.toByteArray(Charsets.UTF_8))

        val result = BankStatementImporter.readLimitedStream(stream, maxBytes = 1024)

        assertNotNull(result)
        assertEquals(sampleOfx, result)
    }

    @Test
    fun testReadLimitedStreamOverLimitReturnsNullSafely() {
        // Gera um fluxo de dados simulando arquivo que excede o limite máximo permitido
        val largeData = ByteArray(2048) { 'A'.code.toByte() }
        val stream = ByteArrayInputStream(largeData)

        // Limite restrito a 500 bytes
        val result = BankStatementImporter.readLimitedStream(stream, maxBytes = 500)

        // Deve retornar null para prevenir OutOfMemoryError
        assertNull("Streams acima do limite máximo devem retornar null sem carregar todo o arquivo", result)
    }
}
