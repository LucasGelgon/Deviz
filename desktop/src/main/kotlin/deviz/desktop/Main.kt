package deviz.desktop

import deviz.core.Convertisseur
import deviz.core.Devise
import deviz.core.Langue
import deviz.core.SourceAvecSecours
import deviz.core.TauxEnLigne
import deviz.core.TauxStatiques
import deviz.core.Traductions
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

private val COULEUR_ACCENT = Color(0x2F, 0x6F, 0xED)
private val COULEUR_FOND = Color(0xF5, 0xF6, 0xFA)
private val COULEUR_CARTE_BORDURE = Color(0xE2, 0xE4, 0xEA)
private val COULEUR_TEXTE_DISCRET = Color(0x6B, 0x6F, 0x76)

fun main() {
    // Essaie d'utiliser le style graphique natif de l'OS (plus joli que le
    // style Swing par defaut). Si ce n'est pas possible, ce n'est pas grave
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ignore: Exception) {
    }

    SwingUtilities.invokeLater {
        FenetreDeviz().isVisible = true
    }
}

/**
 * La fenetre principale de l'application desktop.
 */
class FenetreDeviz : JFrame("Deviz") {

    private val tauxEnLigne = TauxEnLigne()
    private val source = SourceAvecSecours(principale = tauxEnLigne, secours = TauxStatiques)
    private val convertisseur = Convertisseur(source)

    private var langue = Langue.FRANCAIS
    private var textes = Traductions.pour(langue)
    private var derniereMiseAJourReussie = false

    private val labelTitre = JLabel()
    private val boutonLangueFr = JToggleButton("FR")
    private val boutonLangueEn = JToggleButton("EN")

    private val labelMontant = JLabel()
    private val champMontant = JTextField("100")
    private val labelDeviseDepart = JLabel()
    private val listeDeviseSource = JComboBox(Devise.values())
    private val boutonInverser = JButton("⇄")
    private val labelDeviseArrivee = JLabel()
    private val listeDeviseCible = JComboBox(Devise.values())

    private val boutonConvertir = JButton()
    private val boutonRafraichir = JButton()
    private val labelResultat = JLabel(" ", SwingConstants.CENTER)
    private val labelStatutTaux = JLabel(" ", SwingConstants.CENTER)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(460, 480)
        minimumSize = Dimension(420, 440)
        setLocationRelativeTo(null)
        contentPane.background = COULEUR_FOND
        layout = BorderLayout()

        val renduDevise = RenduDevise()
        listeDeviseSource.renderer = renduDevise
        listeDeviseCible.renderer = renduDevise
        listeDeviseSource.selectedItem = Devise.EUR
        listeDeviseCible.selectedItem = Devise.USD

        add(construireEntete(), BorderLayout.NORTH)
        add(construireFormulaire(), BorderLayout.CENTER)
        add(construirePiedDePage(), BorderLayout.SOUTH)

        appliquerTextes()
        rafraichirTaux()
    }

    private fun construireEntete(): JComponent {
        val entete = JPanel(BorderLayout())
        entete.background = COULEUR_FOND
        entete.border = EmptyBorder(20, 24, 8, 24)

        labelTitre.font = Font("SansSerif", Font.BOLD, 26)
        labelTitre.foreground = COULEUR_ACCENT

        val boutonsLangue = JPanel()
        boutonsLangue.isOpaque = false
        boutonsLangue.layout = BoxLayout(boutonsLangue, BoxLayout.X_AXIS)

        val groupe = ButtonGroup()
        groupe.add(boutonLangueFr)
        groupe.add(boutonLangueEn)
        boutonLangueFr.isSelected = true
        boutonLangueFr.addActionListener { changerLangue(Langue.FRANCAIS) }
        boutonLangueEn.addActionListener { changerLangue(Langue.ANGLAIS) }
        boutonsLangue.add(boutonLangueFr)
        boutonsLangue.add(boutonLangueEn)

        entete.add(labelTitre, BorderLayout.WEST)
        entete.add(boutonsLangue, BorderLayout.EAST)
        return entete
    }

    private fun construireFormulaire(): JComponent {
        val carte = JPanel(GridBagLayout())
        carte.background = Color.WHITE
        carte.border = CompoundBorder(
            LineBorder(COULEUR_CARTE_BORDURE, 1, true),
            EmptyBorder(20, 20, 20, 20)
        )

        val conteneur = JPanel(BorderLayout())
        conteneur.background = COULEUR_FOND
        conteneur.border = EmptyBorder(8, 24, 8, 24)
        conteneur.add(carte, BorderLayout.NORTH)

        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(6, 0, 6, 0)
        c.gridx = 0
        c.weightx = 1.0

        c.gridy = 0
        carte.add(labelMontant, c)
        c.gridy = 1
        champMontant.font = champMontant.font.deriveFont(16f)
        carte.add(champMontant, c)

        c.gridy = 2
        carte.add(labelDeviseDepart, c)
        c.gridy = 3
        carte.add(listeDeviseSource, c)

        c.gridy = 4
        c.fill = GridBagConstraints.NONE
        boutonInverser.isFocusPainted = false
        boutonInverser.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        boutonInverser.addActionListener { inverserDevises() }
        carte.add(boutonInverser, c)
        c.fill = GridBagConstraints.HORIZONTAL

        c.gridy = 5
        carte.add(labelDeviseArrivee, c)
        c.gridy = 6
        carte.add(listeDeviseCible, c)

        return conteneur
    }

    private fun construirePiedDePage(): JComponent {
        val pied = JPanel()
        pied.layout = BoxLayout(pied, BoxLayout.Y_AXIS)
        pied.background = COULEUR_FOND
        pied.border = EmptyBorder(4, 24, 20, 24)

        boutonConvertir.font = boutonConvertir.font.deriveFont(Font.BOLD, 15f)
        boutonConvertir.background = COULEUR_ACCENT
        boutonConvertir.foreground = Color.WHITE
        boutonConvertir.isFocusPainted = false
        boutonConvertir.isOpaque = true
        boutonConvertir.alignmentX = Component.CENTER_ALIGNMENT
        boutonConvertir.maximumSize = Dimension(Int.MAX_VALUE, 42)
        boutonConvertir.addActionListener { convertir() }

        labelResultat.font = labelResultat.font.deriveFont(Font.BOLD, 18f)
        labelResultat.alignmentX = Component.CENTER_ALIGNMENT
        labelResultat.border = EmptyBorder(14, 0, 4, 0)

        labelStatutTaux.font = labelStatutTaux.font.deriveFont(12f)
        labelStatutTaux.foreground = COULEUR_TEXTE_DISCRET
        labelStatutTaux.alignmentX = Component.CENTER_ALIGNMENT

        boutonRafraichir.isFocusPainted = false
        boutonRafraichir.isContentAreaFilled = false
        boutonRafraichir.isBorderPainted = false
        boutonRafraichir.foreground = COULEUR_ACCENT
        boutonRafraichir.alignmentX = Component.CENTER_ALIGNMENT
        boutonRafraichir.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        boutonRafraichir.addActionListener { rafraichirTaux() }

        pied.add(boutonConvertir)
        pied.add(labelResultat)
        pied.add(labelStatutTaux)
        pied.add(boutonRafraichir)

        return pied
    }

    private fun changerLangue(nouvelleLangue: Langue) {
        langue = nouvelleLangue
        textes = Traductions.pour(langue)
        appliquerTextes()
    }

    private fun appliquerTextes() {
        labelTitre.text = textes.titre
        labelMontant.text = textes.montant
        labelDeviseDepart.text = textes.deviseDepart
        labelDeviseArrivee.text = textes.deviseArrivee
        boutonConvertir.text = textes.convertir
        boutonInverser.toolTipText = textes.inverser
        boutonRafraichir.text = "↻ " + textes.rafraichirTaux

        // Les JComboBox affichent les objets Devise via RenduDevise
        listeDeviseSource.revalidate()
        listeDeviseSource.repaint()
        listeDeviseCible.revalidate()
        listeDeviseCible.repaint()

        actualiserStatutTaux()
    }

    private fun inverserDevises() {
        val depart = listeDeviseSource.selectedItem
        listeDeviseSource.selectedItem = listeDeviseCible.selectedItem
        listeDeviseCible.selectedItem = depart
    }

    private fun rafraichirTaux() {
        labelStatutTaux.text = textes.chargementTaux
        boutonRafraichir.isEnabled = false

        Thread {
            val succes = try {
                tauxEnLigne.rafraichir()
                true
            } catch (e: Exception) {
                false
            }

            SwingUtilities.invokeLater {
                derniereMiseAJourReussie = succes
                boutonRafraichir.isEnabled = true
                actualiserStatutTaux()
            }
        }.start()
    }

    private fun actualiserStatutTaux() {
        val date = tauxEnLigne.dateDesTaux
        labelStatutTaux.text = if (derniereMiseAJourReussie && date != null) {
            textes.tauxMisAJour.format(date)
        } else {
            textes.tauxHorsLigne
        }
    }

    private fun convertir() {
        val montant = champMontant.text.replace(",", ".").toDoubleOrNull()

        if (montant == null || montant < 0) {
            JOptionPane.showMessageDialog(this, textes.montantInvalide, textes.titre, JOptionPane.ERROR_MESSAGE)
            return
        }

        val depart = listeDeviseSource.selectedItem as Devise
        val arrivee = listeDeviseCible.selectedItem as Devise
        val resultat = convertisseur.convertir(montant, depart, arrivee)

        labelResultat.text = textes.resultat(montant, depart.symbole, resultat, arrivee.symbole)
    }

    /** Affiche chaque Devise dans la langue courante au lieu de son nom Kotlin brut. */
    private inner class RenduDevise : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val composant = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is Devise) {
                text = value.affichage(langue)
            }
            return composant
        }
    }
}
