package deviz.core

/**
 * Convertit un montant d'une devise vers une autre.
 * On passe toujours par l'euro comme "pivot" pour éviter de devoir stocker 
 * un taux pour chaque paire de devises possible.
 */
class Convertisseur(private val source: SourceDeTaux = TauxStatiques) {

    fun convertir(montant: Double, de: Devise, vers: Devise): Double {
        require(montant >= 0) { "Le montant doit etre positif ou nul" }

        val montantEnEuro = montant / source.taux(de)
        return montantEnEuro * source.taux(vers)
    }
}
