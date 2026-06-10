package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Padding inferiore per restare sopra la barra di navigazione di sistema (3 tasti / gesture). */
@Composable
fun Modifier.tsmNavigationBarPadding(): Modifier =
    padding(WindowInsets.navigationBars.asPaddingValues())

/** Padding superiore per la status bar (edge-to-edge). */
@Composable
fun Modifier.tsmStatusBarPadding(): Modifier =
    padding(WindowInsets.statusBars.asPaddingValues())
