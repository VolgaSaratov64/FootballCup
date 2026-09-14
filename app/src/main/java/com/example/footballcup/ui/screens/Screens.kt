package com.example.footballcup.ui.screens

import android.graphics.Color as AColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballcup.data.Match
import com.example.footballcup.data.Player
import com.example.footballcup.data.Team
import com.example.footballcup.logic.TeamState
import com.example.footballcup.ui.MatchUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun teamColor(hex: String?): Color =
    runCatching { Color(AColor.parseColor(hex ?: "#888888")) }.getOrDefault(Color.Gray)

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onTeams: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Футбольный турнир", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, Modifier.fillMaxWidth()) { Text("Начать матч") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onTeams, Modifier.fillMaxWidth()) { Text("Команды и игроки") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onHistory, Modifier.fillMaxWidth()) { Text("История") }
    }
}

@Composable
fun StartMatchScreen(
    teams: List<Team>,
    onStart: (Long, Long) -> Unit,
    onBack: () -> Unit
) {
    var a by remember { mutableStateOf<Long?>(null) }
    var b by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Кто играет первым?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        teams.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = a == t.id, onClick = { a = t.id })
                Box(Modifier.size(14.dp).background(teamColor(t.colorHex)))
                Spacer(Modifier.width(6.dp))
                Text("${t.name} (A)")
                Spacer(Modifier.width(16.dp))
                RadioButton(selected = b == t.id, onClick = { b = t.id })
                Text("${t.name} (B)")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { a?.let { aa -> b?.let { bb -> onStart(aa, bb) } } },
            enabled = a != null && b != null && a != b,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Начать") }
        OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
fun MatchScreen(
    state: MatchUiState,
    playersA: List<Player>,
    playersB: List<Player>,
    onTick: () -> Unit,
    onGoal: (Long, Long, Boolean) -> Unit,
    onUndo: () -> Unit,
    onAdjust: (Int) -> Unit,
    onFinish: () -> Unit,
    onDraw: (Long) -> Unit,
    onNext: () -> Unit,
    onFinishTournament: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.finished) {
        while (!state.finished) { delay(1000); onTick() }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("Матч #${state.matchNumber}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(48.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamScore(state.teamA, state.scoreA)
            Text("—", fontSize = 32.sp)
            TeamScore(state.teamB, state.scoreB)
        }

        Spacer(Modifier.height(12.dp))
        val m = state.secondsLeft / 60
        val s = state.secondsLeft % 60
        Text("%d:%02d".format(m, s), fontSize = 56.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(onClick = { onAdjust(-30) }) { Text("−30с") }
            FilledTonalButton(onClick = { onAdjust(30) }) { Text("+30с") }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GoalButton(state.teamA, playersA, playersB, !state.finished,
                onGoal = { sid, own -> state.teamA?.let { onGoal(it.id, sid, own) } })
            GoalButton(state.teamB, playersB, playersA, !state.finished,
                onGoal = { sid, own -> state.teamB?.let { onGoal(it.id, sid, own) } })
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUndo, enabled = !state.finished && state.goals.isNotEmpty()) {
            Text("Отменить последний гол")
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(state.goals) { g ->
                val team = if (g.scoringTeamId == state.teamA?.id) state.teamA else state.teamB
                Text("${g.minute}'  ${if (g.isOwnGoal) "АВТОГОЛ " else ""}${g.scorerName} → ${team?.name}",
                    Modifier.padding(4.dp))
            }
        }

        if (!state.finished) {
            Button(onClick = onFinish, Modifier.fillMaxWidth()) { Text("Завершить матч") }
        } else if (state.awaitingDrawChoice) {
            Text("Ничья! Кто садится?")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                state.teamA?.let { Button(onClick = { onDraw(it.id) }) { Text(it.name) } }
                state.teamB?.let { Button(onClick = { onDraw(it.id) }) { Text(it.name) } }
            }
        } else {
            Button(onClick = onNext, Modifier.fillMaxWidth()) { Text("Следующий матч") }
        }

        OutlinedButton(
            onClick = onFinishTournament,
            Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Завершить турнир") }
    }
}

@Composable
private fun TeamScore(team: Team?, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(24.dp).background(teamColor(team?.colorHex)))
        Text(team?.name ?: "?", fontWeight = FontWeight.Bold)
        Text("$score", fontSize = 40.sp)
    }
}

@Composable
private fun GoalButton(
    team: Team?, players: List<Player>, opponents: List<Player>,
    enabled: Boolean, onGoal: (Long, Boolean) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    Button(
        onClick = { show = true }, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = teamColor(team?.colorHex))
    ) { Text("Гол: ${team?.name ?: "?"}") }

    if (show && team != null) {
        GoalDialog(players, opponents,
            onDismiss = { show = false },
            onConfirm = { sid, own -> onGoal(sid, own); show = false })
    }
}

@Composable
private fun GoalDialog(
    ownPlayers: List<Player>, oppPlayers: List<Player>,
    onDismiss: () -> Unit, onConfirm: (Long, Boolean) -> Unit
) {
    var ownGoal by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    val list = if (ownGoal) oppPlayers else ownPlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Автор гола") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ownGoal, onCheckedChange = { ownGoal = it; selected = null })
                    Text("Автогол")
                }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(list) { p ->
                        Row(Modifier.fillMaxWidth().padding(8.dp).clickable { selected = p.id },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == p.id, onClick = { selected = p.id })
                            Text(p.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let { onConfirm(it, ownGoal) } },
                enabled = selected != null) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun TeamsScreen(
    teams: List<Team>,
    players: Map<Long, List<Player>>,
    onAddTeam: (String, String) -> Unit,
    onAddPlayer: (Long, String, Int?) -> Unit,
    onDeletePlayer: (Player) -> Unit,
    onBack: () -> Unit
) {
    var showAddTeam by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Команды") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTeam = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(teams, key = { it.id }) { t ->
                TeamCard(t, players[t.id].orEmpty(), onAddPlayer, onDeletePlayer)
            }
        }
    }

    if (showAddTeam) {
        AddTeamDialog({ showAddTeam = false }, { n, c -> onAddTeam(n, c); showAddTeam = false })
    }
}

@Composable
private fun TeamCard(
    team: Team, players: List<Player>,
    onAddPlayer: (Long, String, Int?) -> Unit, onDeletePlayer: (Player) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).background(teamColor(team.colorHex)))
                Spacer(Modifier.width(8.dp))
                Text(team.name, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAdd = true }) { Text("+ Игрок") }
            }
            players.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (p.number != null) "#${p.number} ${p.name}" else p.name)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onDeletePlayer(p) }) { Icon(Icons.Default.Delete, null) }
                }
            }
        }
    }
    if (showAdd) {
        AddPlayerDialog({ showAdd = false }, { n, num -> onAddPlayer(team.id, n, num); showAdd = false })
    }
}

@Composable
private fun AddPlayerDialog(onDismiss: () -> Unit, onConfirm: (String, Int?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var num by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый игрок") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(num, { if (it.all(Char::isDigit)) num = it },
                    label = { Text("Номер") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), num.toIntOrNull()) },
            enabled = name.isNotBlank()) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AddTeamDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#E53935") }
    val palette = listOf("#E53935", "#43A047", "#1E88E5", "#FDD835", "#8E24AA", "#FB8C00")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая команда") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { hex ->
                        Box(Modifier.size(32.dp).background(teamColor(hex))
                            .clickable { color = hex })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), color) },
            enabled = name.isNotBlank()) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun StandingsScreen(standings: List<TeamState>, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Таблица") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            items(standings) { st ->
                Row(Modifier.fillMaxWidth().padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(teamColor(st.team.colorHex)))
                    Spacer(Modifier.width(6.dp))
                    Text(st.team.name, Modifier.weight(1f))
                    Text("И:${st.games}  О:${st.points}  Г:${st.goalsFor}-${st.goalsAgainst}",
                        fontWeight = FontWeight.Bold)
                }
                Divider()
            }
        }
    }
}

@Composable
fun HistoryScreen(
    matches: List<Match>,
    teams: List<Team>,
    topScorers: List<Triple<String, String, Int>>,
    ownGoals: List<Pair<String, Int>>,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Матчи", "Бомбардиры", "Автоголы")
    val df = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val teamsById = teams.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> {
                    if (matches.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Пока нет завершённых матчей")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(matches) { m ->
                                Card {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${teamsById[m.teamAId]?.name ?: "?"}  ${m.scoreA} : ${m.scoreB}  ${teamsById[m.teamBId]?.name ?: "?"}",
                                            fontWeight = FontWeight.Bold)
                                        Text(df.format(Date(m.startedAt)),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (topScorers.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Пока никто не забивал")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(topScorers) { idx, item ->
                                val name = item.first
                                val team = item.second
                                val count = item.third
                                Card {
                                    Row(Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}.", Modifier.width(30.dp),
                                            fontWeight = FontWeight.Bold)
                                        Text("$name ($team)", Modifier.weight(1f))
                                        Text("$count ${pluralGoals(count)}",
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (ownGoals.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Автоголов нет")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ownGoals) { item ->
                                val name = item.first
                                val count = item.second
                                Card {
                                    Row(Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, Modifier.weight(1f))
                                        Text("$count ${pluralGoals(count)}",
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pluralGoals(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "гол"
        mod10 in 2..4 && mod100 !in 12..14 -> "гола"
        else -> "голов"
    }
}
