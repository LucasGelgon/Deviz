package deviz.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Grace a cette interface, le convertisseur ne depend pas d'une source de
 * taux precise : elle peut etre fixe, en direct, ou une combinaison des deux.
 */
interface SourceDeTaux {
    fun taux(devise: Devise): Double
}

/**
 * Taux de change fixes, exprimes par rapport a 1 euro.
 * Exemple : USD = 1.09 signifie que 1 EUR vaut 1.09 USD.
 */
object TauxStatiques : SourceDeTaux {
    private val tauxParRapportAEuro: Map<Devise, Double> = mapOf(
        Devise.EUR to 1.0,
        Devise.USD to 1.09,
        Devise.GBP to 0.85,
        Devise.CHF to 0.94,
        Devise.JPY to 171.50,
        Devise.CAD to 1.47,
        Devise.AUD to 1.62,
        Devise.CNY to 7.85
    )

    override fun taux(devise: Devise): Double =
        tauxParRapportAEuro[devise] ?: error("Taux inconnu pour la devise $devise")
}

/**
 * Va chercher des taux de change reels sur l'API Fraunkfurter
 */
class TauxEnLigne : SourceDeTaux {

    private var cache: Map<Devise, Double> = emptyMap()

    /** Date (fournie par l'API) des taux actuellement en cache, ou null si jamais charges. */
    var dateDesTaux: String? = null
        private set

    fun rafraichir() {
        val autresDevises = Devise.values().filter { it != Devise.EUR }
        val symboles = autresDevises.joinToString(",") { it.name }
        val url = URL("https://api.frankfurter.dev/v1/latest?base=EUR&symbols=$symboles")

        val connexion = url.openConnection() as HttpURLConnection
        connexion.connectTimeout = 5000
        connexion.readTimeout = 5000

        try {
            val corpsReponse = connexion.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(corpsReponse)
            val tauxJson = json.getJSONObject("rates")

            val nouveauCache = mutableMapOf(Devise.EUR to 1.0)
            for (devise in autresDevises) {
                nouveauCache[devise] = tauxJson.getDouble(devise.name)
            }

            cache = nouveauCache
            dateDesTaux = json.getString("date")
        } finally {
            connexion.disconnect()
        }
    }

    override fun taux(devise: Devise): Double =
        cache[devise] ?: error("Les taux en direct n'ont pas encore ete charges")
}

/**
 * Essaie d'abord une source "principale" (ex taux en ligne) et si ça échoue, se rabat sur un taxu statique
 */
class SourceAvecSecours(
    private val principale: SourceDeTaux,
    private val secours: SourceDeTaux
) : SourceDeTaux {

    /** Vrai si le dernier appel a taux() a du utiliser la source de secours. */
    var dernierAppelEnSecours: Boolean = false
        private set

    override fun taux(devise: Devise): Double =
        try {
            val valeur = principale.taux(devise)
            dernierAppelEnSecours = false
            valeur
        } catch (erreur: Exception) {
            dernierAppelEnSecours = true
            secours.taux(devise)
        }
}
