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
                            itemsIndexed(topScorers) { idx, (name, team, count) ->
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
                            items(ownGoals) { (name, count) ->
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
