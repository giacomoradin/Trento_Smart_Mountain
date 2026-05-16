package it.trentosmartmountain.app.repository

/**
 * Marker per i repository di dominio nel layer MVVM.
 *
 * Le implementazioni nascondono Retrofit/Room e espongono Flow o sealed result
 * alla UI, con strategia offline-first dove applicabile.
 */
interface AppRepository
