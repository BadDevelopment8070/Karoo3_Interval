package com.bastiankahuna.karoosmartftp.model

object WorkoutLibrary {
    private fun warmup() = WorkoutSegment(SegmentType.WARMUP, "Warm-up", 10 * 60, 50, note = "Locker einrollen, die letzten 2 Minuten Richtung Zielbereich.")
    private fun cooldown() = WorkoutSegment(SegmentType.COOLDOWN, "Cool-down", 10 * 60, 45, note = "Locker ausrollen.")
    private fun rest(minutes: Int, seconds: Int = 0) = WorkoutSegment(SegmentType.RECOVERY, "Recovery", minutes * 60 + seconds, 50, note = "Pause läuft realzeitbasiert weiter.")
    private fun work(title: String, minutes: Int, seconds: Int = 0, pct: Int, cadLow: Int? = null, cadHigh: Int? = null, note: String = "") =
        WorkoutSegment(SegmentType.WORK, title, minutes * 60 + seconds, pct, cadLow, cadHigh, note)

    private fun repeat(block: List<WorkoutSegment>, times: Int): List<WorkoutSegment> = buildList {
        repeat(times) { addAll(block) }
    }

    val all: List<WorkoutDefinition> = listOf(
        WorkoutDefinition(
            id = "ftp_4x5_100",
            name = "FTP Builder 4x5",
            description = "Klassischer FTP-Reiz. Work-Intervalle zählen nur ab 95% Zielleistung.",
            tags = listOf("FTP", "49min"),
            segments = listOf(warmup()) + repeat(listOf(work("5' FTP", 5, pct = 100), rest(3)), 3) + listOf(work("5' FTP", 5, pct = 100), cooldown())
        ),
        WorkoutDefinition(
            id = "sweetspot_3x10_90",
            name = "Sweet Spot 3x10",
            description = "Hohe Qualität bei kontrollierter Ermüdung.",
            tags = listOf("Sweet Spot", "56min"),
            segments = listOf(warmup()) + repeat(listOf(work("10' Sweet Spot", 10, pct = 90), rest(3)), 2) + listOf(work("10' Sweet Spot", 10, pct = 90), cooldown())
        ),
        WorkoutDefinition(
            id = "ftp_5x4_105",
            name = "FTP Builder 5x4",
            description = "Kurze harte FTP-Blöcke nahe oberer Schwelle.",
            tags = listOf("FTP", "50min"),
            segments = listOf(warmup()) + repeat(listOf(work("4' @105%", 4, pct = 105), rest(2, 30)), 4) + listOf(work("4' @105%", 4, pct = 105), cooldown())
        ),
        WorkoutDefinition(
            id = "overunder_3x9",
            name = "Over/Under 3x9",
            description = "Wechsel aus 95% und 105% FTP, gut für Laktat-Clearance.",
            tags = listOf("FTP", "Over/Under", "55min"),
            segments = listOf(warmup()) + repeat(
                listOf(
                    work("3' Under", 3, pct = 95),
                    work("3' Over", 3, pct = 105),
                    work("3' Under", 3, pct = 95),
                    rest(4)
                ), 2
            ) + listOf(work("3' Under", 3, pct = 95), work("3' Over", 3, pct = 105), work("3' Under", 3, pct = 95), cooldown())
        ),
        WorkoutDefinition(
            id = "vo2_6x3_110",
            name = "VO2 Einstieg 6x3",
            description = "Kurz und hart, noch unter einer Stunde.",
            tags = listOf("VO2", "53min"),
            segments = listOf(warmup()) + repeat(listOf(work("3' VO2", 3, pct = 110), rest(2, 30)), 5) + listOf(work("3' VO2", 3, pct = 110), cooldown())
        ),
        WorkoutDefinition(
            id = "threshold_3x8_100",
            name = "Threshold 3x8",
            description = "Stabiler Schwellenblock mit Straßenverkehr-tauglichem Work-Gating.",
            tags = listOf("FTP", "52min"),
            segments = listOf(warmup()) + repeat(listOf(work("8' Threshold", 8, pct = 100), rest(4)), 2) + listOf(work("8' Threshold", 8, pct = 100), cooldown())
        ),
        WorkoutDefinition(
            id = "microburst_2x10",
            name = "Microburst 2x10",
            description = "30/30s-Wechsel. Auch hier zählt jeder Work-Block nur mit Leistung.",
            tags = listOf("FTP", "Micro", "45min"),
            segments = listOf(warmup()) + repeat((1..10).flatMap { listOf(work("30s ON", 0, 30, 115), rest(0, 30)) } + listOf(rest(5)), 1) +
                    (1..10).flatMap { listOf(work("30s ON", 0, 30, 115), rest(0, 30)) } + listOf(cooldown())
        ),
        WorkoutDefinition(
            id = "tempo_to_ftp_4x6",
            name = "Tempo → FTP 4x6",
            description = "Progression von 88% zu 100% FTP.",
            tags = listOf("Build", "58min"),
            segments = listOf(warmup(), work("6' Tempo", 6, pct = 88), rest(3), work("6' Sweet", 6, pct = 92), rest(3), work("6' SubFTP", 6, pct = 96), rest(3), work("6' FTP", 6, pct = 100), cooldown())
        ),
        WorkoutDefinition(
            id = "k3_low_5x4",
            name = "K3 Low Cadence 5x4",
            description = "Kraftausdauer: 88–92% FTP, 50–60 rpm.",
            tags = listOf("K3", "Low Cadence", "52min"),
            segments = listOf(warmup()) + repeat(listOf(work("4' K3 50–60rpm", 4, pct = 90, cadLow = 50, cadHigh = 60), rest(3)), 4) + listOf(work("4' K3 50–60rpm", 4, pct = 90, cadLow = 50, cadHigh = 60), cooldown())
        ),
        WorkoutDefinition(
            id = "k3_torque_4x6",
            name = "K3 Torque 4x6",
            description = "Längere Low-Cadence-Blöcke bei 85–90% FTP.",
            tags = listOf("K3", "Torque", "53min"),
            segments = listOf(warmup()) + repeat(listOf(work("6' Torque 55–65rpm", 6, pct = 88, cadLow = 55, cadHigh = 65), rest(3)), 3) + listOf(work("6' Torque 55–65rpm", 6, pct = 88, cadLow = 55, cadHigh = 65), cooldown())
        ),
        WorkoutDefinition(
            id = "k3_3x8",
            name = "K3 Strength 3x8",
            description = "3 lange Kraftausdauerblöcke, 55–65 rpm.",
            tags = listOf("K3", "Strength", "58min"),
            segments = listOf(warmup()) + repeat(listOf(work("8' K3 55–65rpm", 8, pct = 87, cadLow = 55, cadHigh = 65), rest(5)), 2) + listOf(work("8' K3 55–65rpm", 8, pct = 87, cadLow = 55, cadHigh = 65), cooldown())
        ),
        WorkoutDefinition(
            id = "k3_ladder",
            name = "K3 Torque Ladder",
            description = "K3-Leiter mit steigender Ziel-Funktion, unter 1h.",
            tags = listOf("K3", "Ladder", "51min"),
            segments = listOf(warmup(), work("4' 85% 60rpm", 4, pct = 85, cadLow = 55, cadHigh = 65), rest(3), work("5' 88% 58rpm", 5, pct = 88, cadLow = 52, cadHigh = 62), rest(3), work("6' 90% 55rpm", 6, pct = 90, cadLow = 50, cadHigh = 60), rest(3), work("5' 88% 58rpm", 5, pct = 88, cadLow = 52, cadHigh = 62), cooldown())
        ),
        WorkoutDefinition(
            id = "ftp_2x15_92",
            name = "Sustained 2x15",
            description = "Zwei lange Sweet-Spot/Schwellen-Blöcke, gut dosierbar.",
            tags = listOf("FTP", "56min"),
            segments = listOf(warmup(), work("15' @92%", 15, pct = 92), rest(6), work("15' @92%", 15, pct = 92), cooldown())
        ),
        WorkoutDefinition(
            id = "ftp_pyramid_60",
            name = "FTP Pyramid",
            description = "5/7/9/7/5 Minuten mit moderater Pause.",
            tags = listOf("FTP", "58min"),
            segments = listOf(warmup(), work("5' @95%", 5, pct = 95), rest(3), work("7' @98%", 7, pct = 98), rest(3), work("9' @100%", 9, pct = 100), rest(3), work("7' @98%", 7, pct = 98), rest(3), work("5' @95%", 5, pct = 95), cooldown())
        ),
        WorkoutDefinition(
            id = "ftp_short_3x5_80",
            name = "Traffic Safe 3x5 @80",
            description = "Dein Beispiel: 3x5min @80% mit 4min Pause.",
            tags = listOf("Base", "Example", "42min"),
            segments = listOf(warmup()) + repeat(listOf(work("5' @80%", 5, pct = 80), rest(4)), 2) + listOf(work("5' @80%", 5, pct = 80), cooldown())
        )
    )

    fun byId(id: String?): WorkoutDefinition = all.firstOrNull { it.id == id } ?: all.first()
}
