package com.companion.yogawellness.data

/** Which dataset a row belongs to: the user's own data or one of the synthetic demo personas. */
object ProfileIds {
    const val MY_ACCOUNT = "MY_ACCOUNT"
    const val PERSONA_A = "PERSONA_A"
    const val PERSONA_B = "PERSONA_B"
    const val PERSONA_C = "PERSONA_C"
    val DEMO_IDS = listOf(PERSONA_A, PERSONA_B, PERSONA_C)

    fun isDemo(profileId: String) = profileId != MY_ACCOUNT
    fun displayName(profileId: String) = when (profileId) {
        MY_ACCOUNT -> "My Account"
        PERSONA_A -> "Demo A · Consistent"
        PERSONA_B -> "Demo B · Exam Gap"
        PERSONA_C -> "Demo C · Null Pattern"
        else -> "My Account"
    }
}
