package com.example.footballcup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.footballcup.data.*
import com.example.footballcup.logic.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GoalUi(
    val scorerId: Long,
    val scoringTeamId: Long,
    val isOwnGoal: Boolean,
    val minute: Int,
    val scorerName: String
)

data class MatchUiState(
    val teamA: Team? = null,
    val teamB: Team? = null,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val secondsLeft: Int = 7 * 60,
    val goals: List<GoalUi> = emptyList(),
    val finished: Boolean = false,
    val awaitingDrawChoice: Boolean = false,
    val matchNumber: Int = 1
)

class TournamentViewModel(app: Application) : AndroidViewModel(app) {

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _players = MutableStateFlow<Map<Long, List<Player>>>(emptyMap())
    val players: StateFlow<Map<Long, List<Player>>> = _players

    private val _matchState = MutableStateFlow(MatchUiState())
    val matchState: StateFlow<MatchUiState> = _matchState

    private val _historyMatches = MutableStateFlow<List<Match>>(emptyList())
    val historyMatches: StateFlow<List<Match>> = _historyMatches

    private var engine: TournamentEngine? = null
    private var matchCounter = 0L
    private var allMatches = mutableListOf<Match>()
    private var currentMatch: Match? = null

    init {
        seedDemoData()
    }

    private fun seedDemoData() {
        val colors = listOf("#E53935" to "Красные", "#43A047" to "Зелёные", "#1E88E5" to "Синие")
        val teams = colors.mapIndexed { i, (c, n) ->
            Team(id = (i + 1).toLong(), name = n, colorHex = c)
        }
        _teams.value = teams

        val playersMap = mutableMapOf<Long, List<Player>>()
        teams.forEach { t ->
            playersMap[t.id] = (1..5).map { i ->
                Player(id = t.id * 100 + i, teamId = t.id, name = "Игрок ${t.name.take(3)} $i", number = i)
            }
        }
        _players.value = playersMap
    }

    fun addTeam(name: String, colorHex: String) {
        val id = (_teams.value.maxOfOrNull { it.id } ?: 0) + 1
        _teams.value = _teams.value + Team(id, name, colorHex)
        _players.value = _players.value + (id to emptyList())
    }

    fun addPlayer(teamId: Long, name: String, number: Int?) {
        val id = System.currentTimeMillis()
        val newP = Player(id, teamId, name, number)
        val cur = _players.value[teamId].orEmpty()
        _players.value = _players.value + (teamId to (cur + newP))
    }

    fun deletePlayer(p: Player) {
        val cur = _players.value[p.teamId].orEmpty().filterNot { it.id == p.id }
        _players.value = _players.value + (p.teamId to cur)
    }

    fun startTournament(aId: Long, bId: Long) {
        engine = TournamentEngine(_teams.value)
        engine!!.startFirstMatch(aId, bId)
        allMatches = mutableListOf()
        matchCounter = 0
        createNextMatch()
    }

    private fun createNextMatch() {
        val e = engine ?: return
        val (aId, bId) = e.currentPair ?: return
        val a = _teams.value.first { it.id == aId }
        val b = _teams.value.first { it.id == bId }
        matchCounter++
        currentMatch = Match(
            id = matchCounter,
            tournamentId = 1L,
            teamAId = aId, teamBId = bId,
            scoreA = 0, scoreB = 0,
            startedAt = System.currentTimeMillis(),
            matchIndex = matchCounter.toInt()
        )
        _matchState.value = MatchUiState(
            teamA = a, teamB = b,
            secondsLeft = 7 * 60,
            matchNumber = matchCounter.toInt()
        )
    }

    fun tick() {
        val s = _matchState.value
        if (s.finished) return
        if (s.secondsLeft <= 0) { finishMatch(); return }
        _matchState.value = s.copy(secondsLeft = s.secondsLeft - 1)
    }

    fun adjustTime(delta: Int) {
        val s = _matchState.value
        _matchState.value = s.copy(secondsLeft = (s.secondsLeft + delta).coerceAtLeast(0))
    }

    fun addGoal(teamId: Long, scorerId: Long, isOwnGoal: Boolean) {
        val s = _matchState.value
        val match = currentMatch ?: return
        val scorerTeamId = _players.value.entries
            .first { (_, list) -> list.any { it.id == scorerId } }.key
        val scoringTeamId = if (isOwnGoal) {
            if (scorerTeamId == match.teamAId) match.teamBId else match.teamAId
        } else teamId

        val minute = ((7 * 60 - s.secondsLeft) / 60).coerceAtLeast(0)
        val scorerName = _players.value.values.flatten().firstOrNull { it.id == scorerId }?.name ?: "?"
        val newA = if (scoringTeamId == match.teamAId) s.scoreA + 1 else s.scoreA
        val newB = if (scoringTeamId == match.teamBId) s.scoreB + 1 else s.scoreB

        _matchState.value = s.copy(
            scoreA = newA, scoreB = newB,
            goals = s.goals + GoalUi(scorerId, scoringTeamId, isOwnGoal, minute, scorerName)
        )

        if (newA >= 2 || newB >= 2) finishMatch()
    }

    fun undoLastGoal() {
        val s = _matchState.value
        if (s.goals.isEmpty() || s.finished) return
        val last = s.goals.last()
        val newA = if (last.scoringTeamId == s.teamA?.id) s.scoreA - 1 else s.scoreA
        val newB = if (last.scoringTeamId == s.teamB?.id) s.scoreB - 1 else s.scoreB
        _matchState.value = s.copy(scoreA = newA, scoreB = newB, goals = s.goals.dropLast(1))
    }

    fun finishMatch(manualLoserId: Long? = null) {
        val s = _matchState.value
        if (s.finished) return
        val match = currentMatch ?: return
        allMatches.add(match.copy(
            scoreA = s.scoreA, scoreB = s.scoreB,
            finishedAt = System.currentTimeMillis()
        ))
        val outcome = MatchOutcome(match.teamAId, match.teamBId, s.scoreA, s.scoreB)
        val needChoice = engine!!.applyOutcome(outcome, manualLoserId)
        _matchState.value = s.copy(finished = true, awaitingDrawChoice = needChoice)
    }

    fun chooseDrawLoser(loserId: Long) {
        val s = _matchState.value
        val match = currentMatch ?: return
        val outcome = MatchOutcome(match.teamAId, match.teamBId, s.scoreA, s.scoreB)
        engine!!.applyOutcome(outcome, manualLoserId = loserId)
        _matchState.value = s.copy(awaitingDrawChoice = false)
    }

    fun nextMatch() { createNextMatch() }

    fun finishTournament() {
        _historyMatches.value = allMatches.toList()
    }

    fun standings(): List<TeamState> = engine?.standings() ?: emptyList()
}
