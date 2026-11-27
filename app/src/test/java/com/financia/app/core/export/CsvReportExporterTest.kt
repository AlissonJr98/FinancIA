package com.financia.app.core.export

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de regressão para sanitização de exportação CSV contra Formula Injection (CWE-1236).
 */
class CsvReportExporterTest {

    @Test
    fun testNormalTextIsNotModified() {
        assertEquals("Aluguel Apartamento", CsvReportExporter.sanitizeCsvCell("Aluguel Apartamento"))
        assertEquals("Supermercado", CsvReportExporter.sanitizeCsvCell("Supermercado"))
        assertEquals("Cartão de Crédito", CsvReportExporter.sanitizeCsvCell("Cartão de Crédito"))
        assertEquals("", CsvReportExporter.sanitizeCsvCell(""))
    }

    @Test
    fun testFormulaInjectionPrefixesAreEscaped() {
        // '=' prefix (Fórmula Excel)
        assertEquals("'=SUM(A1:A10)", CsvReportExporter.sanitizeCsvCell("=SUM(A1:A10)"))
        assertEquals("'=cmd|' /C calc'!A0", CsvReportExporter.sanitizeCsvCell("=cmd|' /C calc'!A0"))

        // '+' prefix
        assertEquals("'+12345", CsvReportExporter.sanitizeCsvCell("+12345"))

        // '-' prefix
        assertEquals("'-2+5*cmd", CsvReportExporter.sanitizeCsvCell("-2+5*cmd"))

        // '@' prefix
        assertEquals("'@SUM(1,2)", CsvReportExporter.sanitizeCsvCell("@SUM(1,2)"))

        // '\t' tab e espaços antes de fórmula são limpos e a fórmula é escapada
        assertEquals("'=cmd", CsvReportExporter.sanitizeCsvCell("\t=cmd"))
        assertEquals("'=cmd", CsvReportExporter.sanitizeCsvCell("   =cmd"))
    }

    @Test
    fun testSemicolonsAndNewlinesAreCleaned() {
        assertEquals("Mercado, Padaria", CsvReportExporter.sanitizeCsvCell("Mercado; Padaria"))
        assertEquals("Nota linha 1  linha 2", CsvReportExporter.sanitizeCsvCell("Nota linha 1\n linha 2\r"))
    }
}
