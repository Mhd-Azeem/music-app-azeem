from pathlib import Path

home = Path("app/src/main/java/com/wavelength/music/ui/home/HomeScreen.kt")
s = home.read_text()

# Imports for floating tile header.
s = s.replace(
    "import androidx.compose.foundation.shape.RoundedCornerShape\n",
    "import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.shape.CircleShape\n"
)

old_top = '''        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = onStatisticsClick,
                        modifier = Modifier.alpha(0.55f)
                    ) {
                        Text("Stats")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
'''
new_top = '''        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Card(
                    onClick = onStatisticsClick,
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                    )
                ) {
                    Text(
                        "Stats",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                Card(
                    onClick = onSettingsClick,
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                    )
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
            }
        }
'''
assert old_top in s, "Home top bar block not found"
s = s.replace(old_top, new_top, 1)
home.write_text(s)

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
n = nav.read_text()

# Imports for floating bottom tiles.
n = n.replace(
    "import androidx.compose.foundation.layout.Column\n",
    "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.Row\nimport androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.foundation.layout.fillMaxWidth\n"
)
n = n.replace(
    "import androidx.compose.material3.TextButton\n",
    "import androidx.compose.material3.TextButton\nimport androidx.compose.material3.Surface\nimport androidx.compose.material3.MaterialTheme\n"
)
n = n.replace(
    "import androidx.compose.ui.Modifier\n",
    "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.Alignment\n"
)
n = n.replace(
    "import androidx.compose.ui.graphics.Color\n",
    "import androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.shape.RoundedCornerShape\n"
)

old_nav = '''                    NavigationBar(
                        modifier = navBarModifier,
                        containerColor = if (isLiquid) Color.Transparent else NavigationBarDefaults.containerColor
                    ) {
                        bottomNavScreens.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(iconFor(screen), contentDescription = null) },
                                label = { Text(navLabelFor(screen)) }
                            )
                        }
                    }
'''
new_nav = '''                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            Surface(
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                                },
                                tonalElevation = if (selected) 8.dp else 4.dp,
                                shadowElevation = if (selected) 8.dp else 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        iconFor(screen),
                                        contentDescription = navLabelFor(screen),
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        navLabelFor(screen),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
'''
assert old_nav in n, "Bottom NavigationBar block not found"
n = n.replace(old_nav, new_nav, 1)

# Remove full-width haze modifier setup so only floating tiles remain.
old_mod = '''                var navBarModifier: Modifier = Modifier
                if (isLiquid) {
                    navBarModifier = navBarModifier.hazeChild(state = hazeState, style = glassStyle) {
                        inputScale = HazeInputScale.Auto
                    }
                }
'''
assert old_mod in n, "navBarModifier block not found"
n = n.replace(old_mod, "", 1)
nav.write_text(n)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Replaced the full-width Home header and bottom navigation bars with compact floating AZ Music, Stats, Settings, Home, Search and Library tiles",\n'
assert needle in a, "About changelog list not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
