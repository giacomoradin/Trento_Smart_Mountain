package it.trentosmartmountain.app.ui.screens.registra

import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Sorgente tile **OpenTopoMap** per OSMdroid (sentieri, curve di livello).
 * Usata da [TsmMapView]; richiede user-agent configurato in [it.trentosmartmountain.app.TsmApplication].
 */
fun openTopoMapTileSource() =
  XYTileSource(
    "OpenTopoMap",
    0,
    17,
    256,
    ".png",
    arrayOf("https://tile.opentopomap.org/"),
    "© OpenStreetMap contributors, SRTM | OpenTopoMap (CC-BY-SA)",
  )
