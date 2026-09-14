from pathlib import Path

settings_path = Path('app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt')
nav_path = Path('app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt')

settings = settings_path.read_text()
nav = nav_path.read_text()

old_sig = '''fun SettingsScreen(
    onBack: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    viewModel: AppSettingsViewModel = hiltViewModel(),'''
new_sig = '''fun SettingsScreen(
    onBack: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onEmailActivationClick: () -> Unit = {},
    viewModel: AppSettingsViewModel = hiltViewModel(),'''
if old_sig not in settings:
    raise SystemExit('SettingsScreen signature anchor not found')
settings = settings.replace(old_sig, new_sig, 1)

about_anchor = '''            item {
                SettingsSection(title = "About") {'''
activation_item = '''            item {
                Button(
                    onClick = onEmailActivationClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Email Activation",
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

'''
if about_anchor not in settings:
    raise SystemExit('About section anchor not found')
settings = settings.replace(about_anchor, activation_item + about_anchor, 1)

old_settings_route = '''                composable(Screen.Settings.route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onStatisticsClick = { navController.navigate(Screen.Statistics.route) }
                        )
                        Button(
                            onClick = { navController.navigate(Screen.Activation.route) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Text("Email Activation")
                        }
                    }
                }'''
new_settings_route = '''                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onStatisticsClick = { navController.navigate(Screen.Statistics.route) },
                        onEmailActivationClick = { navController.navigate(Screen.Activation.route) }
                    )
                }'''
if old_settings_route not in nav:
    raise SystemExit('Floating activation route block not found')
nav = nav.replace(old_settings_route, new_settings_route, 1)

# Alignment was only needed by the removed floating Settings button.
nav = nav.replace('import androidx.compose.ui.Alignment\n', '')

settings_path.write_text(settings)
nav_path.write_text(nav)
print('Email Activation moved into Settings list as a full-width button.')
