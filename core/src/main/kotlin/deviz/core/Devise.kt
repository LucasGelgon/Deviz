package deviz.core

/**
 * La liste des devises que Deviz sait convertir.
 * On pourrait envisager d'en ajouter dans le futur si souhaité
 */
enum class Devise(val libelleFr: String, val libelleEn: String, val symbole: String) {
    EUR("Euro", "Euro", "€"),
    USD("Dollar americain", "US Dollar", "$"),
    GBP("Livre sterling", "British Pound", "£"),
    CHF("Franc suisse", "Swiss Franc", "CHF"),
    JPY("Yen japonais", "Japanese Yen", "¥"),
    CAD("Dollar canadien", "Canadian Dollar", "CA$"),
    AUD("Dollar australien", "Australian Dollar", "AU$"),
    CNY("Yuan chinois", "Chinese Yuan", "CN¥");

    fun libelle(langue: Langue): String = if (langue == Langue.FRANCAIS) libelleFr else libelleEn

    fun affichage(langue: Langue): String = "${libelle(langue)} ($symbole)"
}
