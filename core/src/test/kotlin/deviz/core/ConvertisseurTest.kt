package deviz.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConvertisseurTest {

    private val convertisseur = Convertisseur()

    @Test
    fun `convertir vers la meme devise ne change pas le montant`() {
        val resultat = convertisseur.convertir(100.0, Devise.EUR, Devise.EUR)
        assertEquals(100.0, resultat, 0.0001)
    }

    @Test
    fun `convertir des euros en dollars`() {
        val resultat = convertisseur.convertir(10.0, Devise.EUR, Devise.USD)
        assertEquals(10.9, resultat, 0.0001)
    }

    @Test
    fun `aller puis retour redonne le montant de depart`() {
        val enDollars = convertisseur.convertir(50.0, Devise.EUR, Devise.USD)
        val retourEnEuros = convertisseur.convertir(enDollars, Devise.USD, Devise.EUR)
        assertEquals(50.0, retourEnEuros, 0.0001)
    }

    @Test
    fun `un montant negatif est refuse`() {
        assertFailsWith<IllegalArgumentException> {
            convertisseur.convertir(-5.0, Devise.EUR, Devise.USD)
        }
    }
}

class SourceAvecSecoursTest {

    private object SourceQuiEchoueToujours : SourceDeTaux {
        override fun taux(devise: Devise): Double = error("Pas de reseau")
    }

    @Test
    fun `bascule sur le secours si la source principale echoue`() {
        val source = SourceAvecSecours(principale = SourceQuiEchoueToujours, secours = TauxStatiques)

        val resultat = source.taux(Devise.USD)

        assertEquals(TauxStatiques.taux(Devise.USD), resultat, 0.0001)
        assertTrue(source.dernierAppelEnSecours)
    }

    @Test
    fun `utilise la source principale quand elle fonctionne`() {
        val source = SourceAvecSecours(principale = TauxStatiques, secours = SourceQuiEchoueToujours)

        val resultat = source.taux(Devise.USD)

        assertEquals(TauxStatiques.taux(Devise.USD), resultat, 0.0001)
        assertTrue(!source.dernierAppelEnSecours)
    }
}

class TraductionsTest {

    @Test
    fun `chaque langue a un texte pour chaque cle`() {
        val fr = Traductions.pour(Langue.FRANCAIS)
        val en = Traductions.pour(Langue.ANGLAIS)

        assertTrue(fr.convertir.isNotBlank())
        assertTrue(en.convertir.isNotBlank())
        assertTrue(fr.convertir != en.convertir)
    }

    @Test
    fun `libelle de devise change selon la langue`() {
        assertEquals("Dollar americain", Devise.USD.libelle(Langue.FRANCAIS))
        assertEquals("US Dollar", Devise.USD.libelle(Langue.ANGLAIS))
    }
}
