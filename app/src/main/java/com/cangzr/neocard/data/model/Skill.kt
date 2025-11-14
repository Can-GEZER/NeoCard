package com.cangzr.neocard.data.model

data class Skill(
    val name: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf("name" to name)
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>?): Skill? {
            if (map == null) return null
            val name = map["name"] as? String ?: return null
            return Skill(name = name)
        }
    }
}
