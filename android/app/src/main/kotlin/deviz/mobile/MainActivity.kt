package deviz.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import deviz.core.Convertisseur
import deviz.core.Devise
import deviz.core.Langue
import deviz.core.SourceAvecSecours
import deviz.core.TauxEnLigne
import deviz.core.TauxStatiques
import deviz.core.Traductions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BleuAccent = Color(0xFF2F6FED)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = BleuAccent)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EcranConversion()
                }
            }
        }
    }
}

/**
 * pareil que desktop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranConversion() {
    val tauxEnLigne = remember { TauxEnLigne() }
    val convertisseur = remember { Convertisseur(SourceAvecSecours(tauxEnLigne, TauxStatiques)) }
    val portee = rememberCoroutineScope()

    var langue by remember { mutableStateOf(Langue.FRANCAIS) }
    val textes = Traductions.pour(langue)

    var texteMontant by remember { mutableStateOf("100") }
    var deviseSource by remember { mutableStateOf(Devise.EUR) }
    var deviseCible by remember { mutableStateOf(Devise.USD) }
    var resultat by remember { mutableStateOf<String?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }

    var chargementEnCours by remember { mutableStateOf(true) }
    var derniereMiseAJourReussie by remember { mutableStateOf(false) }

    fun rafraichirTaux() {
        chargementEnCours = true
        // withContext(Dispatchers.IO) deplace l'appel reseau bloquant sur un
        // thread d'arriere-plan : c'est l'equivalent, en coroutines, du
        // Thread { ... } utilise dans l'application desktop.
        portee.launch {
            val succes = try {
                withContext(Dispatchers.IO) { tauxEnLigne.rafraichir() }
                true
            } catch (e: Exception) {
                false
            }
            derniereMiseAJourReussie = succes
            chargementEnCours = false
        }
    }

    LaunchedEffect(Unit) { rafraichirTaux() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = textes.titre,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SelecteurLangue(langue = langue, onLangueChoisie = { langue = it })
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = texteMontant,
                    onValueChange = { texteMontant = it },
                    label = { Text(textes.montant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SelecteurDevise(
                    label = textes.deviseDepart,
                    langue = langue,
                    devise = deviseSource,
                    onDeviseChoisie = { deviseSource = it }
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(onClick = {
                        val temporaire = deviseSource
                        deviseSource = deviseCible
                        deviseCible = temporaire
                    }) {
                        Text("⇄", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                SelecteurDevise(
                    label = textes.deviseArrivee,
                    langue = langue,
                    devise = deviseCible,
                    onDeviseChoisie = { deviseCible = it }
                )
            }
        }

        Button(
            onClick = {
                val montant = texteMontant.replace(",", ".").toDoubleOrNull()
                if (montant == null || montant < 0) {
                    erreur = textes.montantInvalide
                    resultat = null
                } else {
                    erreur = null
                    val valeur = convertisseur.convertir(montant, deviseSource, deviseCible)
                    resultat = textes.resultat(montant, deviseSource.symbole, valeur, deviseCible.symbole)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(textes.convertir, fontSize = 16.sp)
        }

        erreur?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        resultat?.let {
            Text(
                it,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (chargementEnCours) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (derniereMiseAJourReussie && tauxEnLigne.dateDesTaux != null) {
                        textes.tauxMisAJour.format(tauxEnLigne.dateDesTaux)
                    } else {
                        textes.tauxHorsLigne
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = { rafraichirTaux() }, enabled = !chargementEnCours) {
                Text(textes.rafraichirTaux)
            }
        }
    }
}

@Composable
private fun SelecteurLangue(langue: Langue, onLangueChoisie: (Langue) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BoutonLangue("FR", selectionne = langue == Langue.FRANCAIS) { onLangueChoisie(Langue.FRANCAIS) }
        Spacer(modifier = Modifier.size(8.dp))
        BoutonLangue("EN", selectionne = langue == Langue.ANGLAIS) { onLangueChoisie(Langue.ANGLAIS) }
    }
}

@Composable
private fun BoutonLangue(texte: String, selectionne: Boolean, onClick: () -> Unit) {
    val contenu = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    if (selectionne) {
        Button(onClick = onClick, contentPadding = contenu) { Text(texte) }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = contenu) { Text(texte) }
    }
}

/** Menu deroulant Material3 affichant chaque devise dans la langue courante. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurDevise(
    label: String,
    langue: Langue,
    devise: Devise,
    onDeviseChoisie: (Devise) -> Unit
) {
    var ouvert by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = ouvert, onExpandedChange = { ouvert = it }) {
        OutlinedTextField(
            value = devise.affichage(langue),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ouvert) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            Devise.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.affichage(langue)) },
                    onClick = {
                        onDeviseChoisie(option)
                        ouvert = false
                    }
                )
            }
        }
    }
}
