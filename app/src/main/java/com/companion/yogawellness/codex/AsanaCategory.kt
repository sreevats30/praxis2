package com.companion.yogawellness.codex

/** The six Learn-tab sections (6 / 6 / 6 / 5 / 5 / 4 = 32 poses). */
enum class AsanaCategory(val title: String, val subtitle: String, val shortTitle: String) {
    DESK("Desk Worker & Tech-Neck Relief", "Undo the shapes of long sitting and screens", "Desk"),
    MORNING("Morning Energy & Sun Salutation Flow", "Wake the whole body gently", "Morning"),
    STANDING("Standing Strength & Core Power", "Build steady legs and a strong centre", "Standing"),
    BALANCE("Balance, Focus & Neuromuscular Stability", "Train steadiness and attention", "Balance"),
    HIPS("Hip Openers & Lower-Body Mobility", "Ease stiff hips, legs and ankles", "Hips"),
    EVENING("Evening Wind-Down, Stress Relief & Sleep Prep", "Slow down before rest", "Evening")
}
