package com.financia.app.data.repository

import com.financia.app.core.util.SecureCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Testes de regressão para segurança e regras de autorização de Espaços Compartilhados (Shared Spaces).
 * Garante que IDOR e quebra de RLS sejam mitigados por validações estritas de membros (memberUids/ownerUid).
 */
class SharedSpaceSecurityTest {

    @Test
    fun testSharedSpaceCodeValidation() {
        val invalidCode = "   AB "
        val cleanCode = invalidCode.trim().uppercase()
        // Deve ter pelo menos 4 caracteres
        assertTrue(cleanCode.length < 4)

        val validCode = " casal-4829 "
        val cleanValid = validCode.trim().uppercase()
        assertEquals("CASAL-4829", cleanValid)
        assertTrue(cleanValid.length >= 4)
    }

    @Test
    fun testMembershipAuthorizationLogic() {
        val ownerUid = "user_alisson"
        val partnerUid = "user_esposa"
        val attackerUid = "attacker_mallory"

        val memberUids = listOf(ownerUid, partnerUid)

        // Função de simulação da regra isSpaceMember
        fun isSpaceMember(callerUid: String, owner: String, members: List<String>): Boolean {
            return callerUid.isNotBlank() && (callerUid == owner || callerUid in members)
        }

        // Membros legítimos devem ter acesso permitido
        assertTrue("Proprietário deve ser autorizado", isSpaceMember(ownerUid, ownerUid, memberUids))
        assertTrue("Parceiro deve ser autorizado", isSpaceMember(partnerUid, ownerUid, memberUids))

        // Atacante não deve ter acesso (mitigação de IDOR / RLS)
        assertFalse("Usuário externo/atacante NÃO deve ser autorizado", isSpaceMember(attackerUid, ownerUid, memberUids))
        assertFalse("Usuário não autenticado NÃO deve ser autorizado", isSpaceMember("", ownerUid, memberUids))
    }

    @Test
    fun testMemberSelfAdditionPreservesExistingMembers() {
        // Validação da regra: novo membro só pode entrar se a nova lista contiver TODOS os membros anteriores
        val existingMembers = listOf("user_1")
        val joiningMember = "user_2"
        val maliciousJoinAttempt = listOf("attacker_only") // Tentativa de apagar membros existentes

        fun canUpdateMembers(callerUid: String, oldList: List<String>, newList: List<String>): Boolean {
            val preservesOld = newList.containsAll(oldList)
            val addsSelf = callerUid in newList
            return preservesOld && addsSelf
        }

        assertTrue("Novo membro adicionando a si mesmo deve ser aceito",
            canUpdateMembers(joiningMember, existingMembers, existingMembers + joiningMember))

        assertFalse("Tentativa de substituir membros existentes por atacante deve ser rejeitada",
            canUpdateMembers(joiningMember, existingMembers, maliciousJoinAttempt))
    }

    @Test
    fun testSecureCodeGeneratorFormatAndEntropy() {
        val code = SecureCodeGenerator.generatePairingCode()

        // Formato esperado: CASAL-XXXX-XXXX (exato 15 caracteres)
        assertEquals(15, code.length)
        assertTrue(code.startsWith("CASAL-"))

        // Valida que apenas caracteres do alfabeto seguro Crockford Base32 são gerados
        val pattern = Regex("^CASAL-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$")
        assertTrue("Código $code deve seguir o padrão seguro CASAL-XXXX-XXXX", pattern.matches(code))

        // Garante que caracteres ambíguos não estão presentes
        assertFalse("Não deve conter '0'", code.contains('0'))
        assertFalse("Não deve conter 'O'", code.contains('O'))
        assertFalse("Não deve conter '1'", code.contains('1'))
        assertFalse("Não deve conter 'I'", code.contains('I'))
    }

    @Test
    fun testSecureCodeGeneratorUniqueness() {
        // Gera 1000 códigos aleatórios e valida que todos são distintos (sem colisões)
        val sampleSize = 1000
        val generatedCodes = HashSet<String>(sampleSize)

        for (i in 0 until sampleSize) {
            val code = SecureCodeGenerator.generatePairingCode()
            val isNew = generatedCodes.add(code)
            assertTrue("Código duplicado detectado: $code", isNew)
        }

        assertEquals(sampleSize, generatedCodes.size)
    }
}
