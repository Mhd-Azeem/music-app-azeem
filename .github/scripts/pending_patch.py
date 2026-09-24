from pathlib import Path

home = Path("app/src/main/java/com/wavelength/music/ui/home/HomeScreen.kt")
s = home.read_text()

# Imports for reference-style purple glass tiles.
for old, new in [
    ("import androidx.compose.foundation.clickable\n", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.border\n"),
    ("import androidx.compose.ui.draw.alpha\n", "import androidx.compose.ui.draw.alpha\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.shadow\n"),
    ("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\n")
]:
    if new not in s:
        s = s.replace(old, new, 1)

old_header = '''            Row(
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
            }'''
new_header = '''            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val purpleGlassBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xCC332060),
                        Color(0xCC56367F),
                        Color(0xCC764595)
                    )
                )
                val glassBorder = Color.White.copy(alpha = 0.48f)

                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(23.dp))
                        .clip(RoundedCornerShape(23.dp))
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, RoundedCornerShape(23.dp))
                        .clickable(onClick = onStatisticsClick)
                        .padding(horizontal = 17.dp, vertical = 11.dp)
                ) {
                    Text(
                        "Stats",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }'''
assert old_header in s, "Current floating Home header not found"
s = s.replace(old_header, new_header, 1)
home.write_text(s)

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
n = nav.read_text()

# Imports for reference-style purple glass nav tiles.
for old, new in [
    ("import androidx.compose.foundation.background\n", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.clickable\n"),
    ("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.shadow\n")
]:
    if new not in n:
        n = n.replace(old, new, 1)

old_nav = '''                    Row(
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
                    }'''
new_nav = '''                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val purpleGlassBrush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xCC332060),
                                Color(0xCC56367F),
                                Color(0xCC764595)
                            )
                        )
                        bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val tileShape = RoundedCornerShape(24.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(if (selected) 15.dp else 10.dp, tileShape)
                                    .clip(tileShape)
                                    .background(purpleGlassBrush)
                                    .border(
                                        if (selected) 1.6.dp else 1.2.dp,
                                        Color.White.copy(alpha = if (selected) 0.62f else 0.42f),
                                        tileShape
                                    )
                                    .clickable {
                                        if (!selected) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        iconFor(screen),
                                        contentDescription = navLabelFor(screen),
                                        tint = Color.White
                                    )
                                    Text(
                                        navLabelFor(screen),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }'''
assert old_nav in n, "Current floating bottom navigation not found"
n = n.replace(old_nav, new_nav, 1)
nav.write_text(n)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Restyled the floating Home header and bottom navigation as purple translucent glass pills with thin luminous white rims and matching circular controls",\n'
assert needle in a, "About latestUpdates list not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
