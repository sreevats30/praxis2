package com.companion.yogawellness.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The dataset currently being *viewed* (dashboard, history). In-memory only, held by
 * [com.companion.yogawellness.YogaWellnessApp]: every cold start begins on My Account, so a demo
 * never "sticks" after a relaunch. Writes of real user data never consult this — the repository
 * always writes to [ProfileIds.MY_ACCOUNT].
 */
class ActiveProfile {
    private val _id = MutableStateFlow(ProfileIds.MY_ACCOUNT)
    val id: StateFlow<String> = _id.asStateFlow()
    fun select(profileId: String) { _id.value = profileId }
    fun returnToMyAccount() { _id.value = ProfileIds.MY_ACCOUNT }
}
