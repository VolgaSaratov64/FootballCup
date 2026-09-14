package com.example.footballcup.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.footballcup.logic.MatchAlarm
import com.example.footballcup.ui.screens.*

sealed class Screen {
    object Home : Screen()
    object Teams : Screen()
    object History : Screen()
    object Standings : Screen()
    object Match : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
fun AppRoot(vm: TournamentViewModel = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val teams by vm.teams.collectAsState()
    val players by vm.players.collectAsState()
    val matchState by vm.matchState.collectAsState()
    val matches by vm.historyMatches.collectAsState()
    val ctx = LocalContext.current

    when (screen) {
        Screen.Home -> HomeScreen(
            onStart = { screen = Screen.Match },
            onTeams = { screen = Screen.Teams },
            onHistory = { screen = Screen.History }
        )

        Screen.Teams -> TeamsScreen(
            teams = teams, players = players,
            onAddTeam = vm::addTeam,
            onAddPlayer = vm::addPlayer,
            onDeletePlayer = vm::deletePlayer,
            onBack = { screen = Screen.Home }
        )

        Screen.Match -> {
            if (matchState.teamA == null) {
                StartMatchScreen(
                    teams = teams,
                    onStart = { a, b -> vm.startTournament(a, b) },
                    onBack = { screen = Screen.Home }
                )
            } else {
                KeepScreenOn()
                MatchScreen(
                    state = matchState,
                    playersA = players[matchState.teamA!!.id].orEmpty(),
                    playersB = players[matchState.teamB!!.id].orEmpty(),
                    onTick = {
                        val before = matchState.secondsLeft
                        vm.tick()
                        if (before == 1 && vm.matchState.value.secondsLeft == 0) {
                            MatchAlarm.signalEnd(ctx)
                        }
                    },
                    onGoal = vm::addGoal,
                    onUndo = vm::undoLastGoal,
                    onAdjust = vm::adjustTime,
                    onFinish = { vm.finishMatch() },
                    onDraw = { loserId ->
                        vm.chooseDrawLoser(loserId)
                        vm.nextMatch()
                    },
                    onNext = vm::nextMatch,
                    onFinishTournament = {
                        vm.finishTournament()
                        screen = Screen.Standings
                    },
                    onBack = { screen = Screen.Home }
                )
            }
        }

        Screen.Standings -> StandingsScreen(
            standings = vm.standings(),
            onBack = { screen = Screen.Home }
        )

        Screen.History -> HistoryScreen(
            matches = matches,
            teams = teams,
            topScorers = vm.topScorers(),
            ownGoals = vm.ownGoalsList(),
            onBack = { screen = Screen.Home }
        )
    }
}
