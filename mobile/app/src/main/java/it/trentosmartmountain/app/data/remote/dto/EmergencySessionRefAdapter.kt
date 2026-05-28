package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * `sessionId` nel backend può essere ObjectId string (GET lista legacy) o oggetto popolato (POST/PATCH).
 */
class EmergencySessionRefAdapter :
  JsonDeserializer<EmergencySessionRef>,
  JsonSerializer<EmergencySessionRef> {

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext,
  ): EmergencySessionRef {
    return when {
      json.isJsonObject -> context.deserialize(json, EmergencySessionRef::class.java)
      json.isJsonPrimitive && json.asJsonPrimitive.isString ->
        EmergencySessionRef(id = json.asString)
      else -> throw IllegalStateException("sessionId JSON non supportato: $json")
    }
  }

  override fun serialize(
    src: EmergencySessionRef,
    typeOfSrc: Type,
    context: JsonSerializationContext,
  ): JsonElement = context.serialize(src)
}
