package it.trentosmartmountain.app.data.checklist

import it.trentosmartmountain.app.data.remote.dto.ChecklistDto
import it.trentosmartmountain.app.viewmodel.SessionDetailViewModel

object ChecklistMapper {

    private val livelloOrder = listOf("base", "consigliato", "opzionale")

    /** Numero di item visibili in copertina per sezione (espandibili). */
    const val INITIAL_SECTION_VISIBLE = 5

    enum class Priorita { ESSENZIALE, CONSIGLIATO, OPZIONALE, PERSONALE }

    fun priorita(item: SessionDetailViewModel.ChecklistItem): Priorita = when {
        item.isPersonal -> Priorita.PERSONALE
        item.livello.equals("base", ignoreCase = true) -> Priorita.ESSENZIALE
        item.livello.equals("consigliato", ignoreCase = true) -> Priorita.CONSIGLIATO
        item.livello.equals("opzionale", ignoreCase = true) -> Priorita.OPZIONALE
        else -> Priorita.ESSENZIALE
    }

    fun partition(items: List<SessionDetailViewModel.ChecklistItem>): ChecklistSections {
        val essenziali = mutableListOf<SessionDetailViewModel.ChecklistItem>()
        val consigliati = mutableListOf<SessionDetailViewModel.ChecklistItem>()
        val opzionali = mutableListOf<SessionDetailViewModel.ChecklistItem>()
        val personali = mutableListOf<SessionDetailViewModel.ChecklistItem>()
        items.forEach { item ->
            when (priorita(item)) {
                Priorita.ESSENZIALE -> essenziali.add(item)
                Priorita.CONSIGLIATO -> consigliati.add(item)
                Priorita.OPZIONALE -> opzionali.add(item)
                Priorita.PERSONALE -> personali.add(item)
            }
        }
        return ChecklistSections(essenziali, consigliati, opzionali, personali)
    }

    data class ChecklistSections(
        val essenziali: List<SessionDetailViewModel.ChecklistItem>,
        val consigliati: List<SessionDetailViewModel.ChecklistItem>,
        val opzionali: List<SessionDetailViewModel.ChecklistItem>,
        val personali: List<SessionDetailViewModel.ChecklistItem>,
    )

    fun stableServerId(categoria: String, livello: String, nome: String): String {
        val raw = "$categoria|$livello|$nome"
        return "srv_${raw.hashCode().and(0x7FFFFFFF)}"
    }

    fun flattenServerItems(dto: ChecklistDto): List<SessionDetailViewModel.ChecklistItem> {
        val result = mutableListOf<SessionDetailViewModel.ChecklistItem>()
        for (livello in livelloOrder) {
            dto.categorie
                .filter { it.livello.equals(livello, ignoreCase = true) }
                .forEach { cat ->
                    cat.items.forEach { item ->
                        result.add(
                            SessionDetailViewModel.ChecklistItem(
                                id = stableServerId(cat.nome, cat.livello, item.nome),
                                text = item.nome,
                                motivo = item.motivo?.takeIf { it.isNotBlank() },
                                livello = cat.livello,
                                categoria = cat.nome,
                                isPersonal = false,
                            ),
                        )
                    }
                }
        }
        return result
    }

    fun merge(
        serverItems: List<SessionDetailViewModel.ChecklistItem>,
        personal: ChecklistPersonalStore.Snapshot,
    ): List<SessionDetailViewModel.ChecklistItem> {
        val checked = personal.checkedIds
        val mergedServer = serverItems.map { it.copy(checked = it.id in checked) }
        val personalItems = personal.personalItems.map { p ->
            SessionDetailViewModel.ChecklistItem(
                id = p.id,
                text = p.text,
                checked = p.id in checked,
                isPersonal = true,
            )
        }
        val combined = mergedServer + personalItems
        if (personal.itemOrder.isEmpty()) return combined

        val byId = combined.associateBy { it.id }
        val ordered = personal.itemOrder.mapNotNull { byId[it] }.toMutableList()
        combined.filter { it.id !in ordered.map { o -> o.id }.toSet() }.forEach { ordered.add(it) }
        return ordered
    }

    fun toPersonalSnapshot(items: List<SessionDetailViewModel.ChecklistItem>): ChecklistPersonalStore.Snapshot {
        return ChecklistPersonalStore.Snapshot(
            checkedIds = items.filter { it.checked }.map { it.id }.toSet(),
            personalItems = items.filter { it.isPersonal }.map {
                ChecklistPersonalStore.PersonalItem(id = it.id, text = it.text)
            },
            itemOrder = items.map { it.id },
        )
    }
}
