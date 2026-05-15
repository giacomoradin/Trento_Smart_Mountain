package it.trentosmartmountain.app.ui.screens.registra

import org.osmdroid.tileprovider.tilesource.XYTileSource

/** Tile outdoor per escursionismo (sentieri e curve di livello). */
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
