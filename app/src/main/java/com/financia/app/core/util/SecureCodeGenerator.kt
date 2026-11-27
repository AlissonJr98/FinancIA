package com.financia.app.core.util

import java.security.SecureRandom

/**
 * Gerador criptograficamente seguro de tokens e códigos de pareamento para Espaços Compartilhados.
 * Utiliza SecureRandom e alfabeto Base32 legível (sem caracteres ambíguos como 0/O, 1/I).
 */
object SecureCodeGenerator {

    // Alfabeto Base32 legível: 32 caracteres seguros
    private const val CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val secureRandom = SecureRandom()

    /**
     * Gera um código de pareamento no formato "CASAL-XXXX-XXXX" (ex: "CASAL-7K9M-X2P4").
     * Fornece 40 bits de entropia criptográfica (> 1 trilhão de combinações possíveis),
     * mitigando ataques de enumeração por força bruta.
     */
    fun generatePairingCode(): String {
        val part1 = StringBuilder(4)
        val part2 = StringBuilder(4)

        for (i in 0 until 4) {
            val idx = secureRandom.nextInt(CHARSET.length)
            part1.append(CHARSET[idx])
        }

        for (i in 0 until 4) {
            val idx = secureRandom.nextInt(CHARSET.length)
            part2.append(CHARSET[idx])
        }

        return "CASAL-$part1-$part2"
    }
}
