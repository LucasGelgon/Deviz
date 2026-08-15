package deviz.core

/** Les langues disponibles dans l'interface de Deviz. */
enum class Langue {
    FRANCAIS,
    ANGLAIS
}

/**
 * Tous les textes affiches par l'application, pour une langue donnee.
 */
data class Textes(
    val titre: String,
    val montant: String,
    val deviseDepart: String,
    val deviseArrivee: String,
    val convertir: String,
    val inverser: String,
    val rafraichirTaux: String,
    val chargementTaux: String,
    val tauxMisAJour: String,
    val tauxHorsLigne: String,
    val montantInvalide: String
) {
    fun resultat(montant: Double, symboleDepart: String, resultat: Double, symboleArrivee: String): String =
        "%.2f %s = %.2f %s".format(montant, symboleDepart, resultat, symboleArrivee)
}

object Traductions {
    val francais = Textes(
        titre = "Deviz",
        montant = "Montant",
        deviseDepart = "Devise de depart",
        deviseArrivee = "Devise d'arrivee",
        convertir = "Convertir",
        inverser = "Inverser",
        rafraichirTaux = "Rafraichir les taux",
        chargementTaux = "Chargement des taux en direct...",
        tauxMisAJour = "Taux en direct du %s (Banque Centrale Europeenne)",
        tauxHorsLigne = "Hors ligne : taux approximatifs utilises",
        montantInvalide = "Merci d'entrer un montant valide (nombre positif)."
    )

    val anglais = Textes(
        titre = "Deviz",
        montant = "Amount",
        deviseDepart = "From currency",
        deviseArrivee = "To currency",
        convertir = "Convert",
        inverser = "Swap",
        rafraichirTaux = "Refresh rates",
        chargementTaux = "Loading live rates...",
        tauxMisAJour = "Live rates as of %s (European Central Bank)",
        tauxHorsLigne = "Offline: using approximate rates",
        montantInvalide = "Please enter a valid amount (positive number)."
    )

    fun pour(langue: Langue): Textes = when (langue) {
        Langue.FRANCAIS -> francais
        Langue.ANGLAIS -> anglais
    }
}
