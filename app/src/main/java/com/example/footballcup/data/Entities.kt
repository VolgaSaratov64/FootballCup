package com.example.footballcup.data

data class Team(
    val id: Long,
    val name: String,
    val colorHex: String
)

data class Player(
    val id: Long,
    val teamId: Long,
    val name: String,
    val number: Int? = null
)

data class Match(
    val id: Long,
    val tournamentId: Long,
    val teamAId: Long,
    val teamBId: Long,
    val scoreA: Int,
    val scoreB: Int,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val matchIndex: Int
)

data class Goal(
    val id: Long,
    val matchId: Long,
    val scorerId: Long,
    val scorerTeamId: Long,
    val scoringTeamId: Long,
    val isOwnGoal: Boolean,
    val minute: Int
)
