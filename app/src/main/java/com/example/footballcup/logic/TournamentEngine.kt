package com.example.footballcup.logic

import com.example.footballcup.data.Team

data class TeamState(
    val team: Team,
    var gamesInRow: Int = 0,
    var points: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0
) {
    val goalDiff: Int get() = goalsFor - goalsAgainst
    val games: Int get() = wins + draws + losses
}

data class MatchOutcome(
    val teamAId: Long, val teamBId: Long,
    val scoreA: Int, val scoreB: Int
) {
    val winnerId: Long? get() = when {
        scoreA > scoreB -> teamAId
        scoreB > scoreA -> teamBId
        else -> null
    }
    val loserId: Long? get() = when {
        scoreA > scoreB -> teamBId
        scoreB > scoreA -> teamAId
        else -> null
    }
}

class TournamentEngine(private val teams: List<Team>) {

    private val states: MutableMap<Long, TeamState> =
        teams.associate { it.id to TeamState(it) }.toMutableMap()

    var currentPair: Pair<Long, Long>? = null
        private set

    /** Сколько матчей уже завершено в этом турнире */
    private var matchesPlayed = 0

    fun getState(teamId: Long): TeamState = states.getValue(teamId)
    fun allStates(): List<TeamState> = states.values.toList()

    fun startFirstMatch(a: Long, b: Long) { currentPair = a to b }

    /**
     * Применить результат матча.
     * Возвращает true, если нужно спросить пользователя "кто садится"
     * (только при ничьей в самой первой игре турнира).
     */
    fun applyOutcome(outcome: MatchOutcome, manualLoserId: Long? = null): Boolean {
        // 1. Обновляем статистику
        val a = states.getValue(outcome.teamAId)
        val b = states.getValue(outcome.teamBId)
        a.gamesInRow++
        b.gamesInRow++
        a.goalsFor += outcome.scoreA
        a.goalsAgainst += outcome.scoreB
        b.goalsFor += outcome.scoreB
        b.goalsAgainst += outcome.scoreA

        when {
            outcome.scoreA > outcome.scoreB -> { a.points += 3; a.wins++; b.losses++ }
            outcome.scoreB > outcome.scoreA -> { b.points += 3; b.wins++; a.losses++ }
            else -> { a.points += 1; b.points += 1; a.draws++; b.draws++ }
        }

        val isFirstMatch = matchesPlayed == 0
        matchesPlayed++

        // 2. Определяем, кто садится
        if (outcome.winnerId == null) {
            // Ничья
            if (isFirstMatch) {
                // Первый матч турнира — спрашиваем пользователя
                if (manualLoserId == null) return true
                val staying = if (manualLoserId == outcome.teamAId) outcome.teamBId
                              else outcome.teamAId
                rotate(manualLoserId, staying)
            } else {
                // Не первый матч — решаем автоматически
                val autoSit = determineAutoSitter(outcome.teamAId, outcome.teamBId)
                val staying = if (autoSit == outcome.teamAId) outcome.teamBId
                              else outcome.teamAId
                rotate(autoSit, staying)
            }
        } else {
            // Есть победитель — проигравший садится
            rotate(outcome.loserId!!, outcome.winnerId!!)
        }
        return false
    }

    /**
     * Кто садится при ничьей (не первый матч).
     * Правило: кто сыграл больше матчей подряд — тот и садится.
     * Если счётчик равен — садится команда A (произвольный выбор).
     */
    private fun determineAutoSitter(teamAId: Long, teamBId: Long): Long {
        val aRow = states.getValue(teamAId).gamesInRow
        val bRow = states.getValue(teamBId).gamesInRow
        return when {
            aRow > bRow -> teamAId
            bRow > aRow -> teamBId
            else -> teamAId // равные — по договорённости садится A
        }
    }

    /**
     * Сидящий уходит отдыхать (gamesInRow = 0).
     * Оставшийся проверяется: если у него уже 2 игры подряд — он тоже садится,
     * а на поле выходит третья команда.
     */
    private fun rotate(sittingId: Long, stayingId: Long) {
        states[sittingId]!!.gamesInRow = 0
        val staying = states[stayingId]!!
        val thirdId = states.keys.first { it != sittingId && it != stayingId }

        currentPair = when {
            staying.gamesInRow >= 2 -> {
                staying.gamesInRow = 0
                thirdId to sittingId
            }
            else -> stayingId to thirdId
        }
    }

    fun standings(): List<TeamState> =
        states.values.sortedWith(
            compareByDescending<TeamState> { it.points }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsFor }
        )
}
