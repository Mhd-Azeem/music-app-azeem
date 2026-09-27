from pathlib import Path

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
ns = nav.read_text()

state_anchor = '''    var showActivationPrompt by remember { mutableStateOf(false) }
'''
state_repl = '''    var showActivationPrompt by remember { mutableStateOf(false) }
    var searchFocusRequestSequence by remember { mutableFloatStateOf(0f) }
'''
assert state_anchor in ns, "NavGraph state anchor not found"
ns = ns.replace(state_anchor, state_repl, 1)

click_old = '''                                            .clickable {
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
'''
click_new = '''                                            .clickable {
                                                if (selected && screen.route == Screen.Search.route) {
                                                    searchFocusRequestSequence += 1f
                                                } else if (!selected) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
'''
assert click_old in ns, "Bottom nav click handler not found"
ns = ns.replace(click_old, click_new, 1)

search_call = '''                    SearchScreen(
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
'''
search_call_new = '''                    SearchScreen(
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
                        focusRequestSequence = searchFocusRequestSequence.toInt(),
'''
assert search_call in ns, "SearchScreen call anchor not found"
ns = ns.replace(search_call, search_call_new, 1)
nav.write_text(ns)

search = Path("app/src/main/java/com/wavelength/music/ui/search/SearchScreen.kt")
ss = search.read_text()

if "import androidx.compose.ui.focus.FocusRequester\n" not in ss:
    ss = ss.replace(
        "import androidx.compose.ui.Alignment\n",
        "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.focus.FocusRequester\nimport androidx.compose.ui.focus.focusRequester\n",
        1
    )
if "import androidx.compose.ui.platform.LocalSoftwareKeyboardController\n" not in ss:
    ss = ss.replace(
        "import androidx.compose.ui.platform.LocalDensity\n",
        "import androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.ui.platform.LocalSoftwareKeyboardController\n",
        1
    )
if "import kotlinx.coroutines.delay\n" not in ss:
    ss = ss.replace(
        "import java.util.Locale\n",
        "import java.util.Locale\nimport kotlinx.coroutines.delay\n",
        1
    )

sig_old = '''fun SearchScreen(
    onTrackClick: () -> Unit,
    onSwipeToHome: () -> Unit = {},
'''
sig_new = '''fun SearchScreen(
    onTrackClick: () -> Unit,
    focusRequestSequence: Int = 0,
    onSwipeToHome: () -> Unit = {},
'''
assert sig_old in ss, "SearchScreen signature not found"
ss = ss.replace(sig_old, sig_new, 1)

context_anchor = '''    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.consumePendingSearch() }
'''
context_repl = '''    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { viewModel.consumePendingSearch() }
    LaunchedEffect(focusRequestSequence) {
        if (focusRequestSequence > 0) {
            delay(60)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
'''
assert context_anchor in ss, "SearchScreen context anchor not found"
ss = ss.replace(context_anchor, context_repl, 1)

field_mod = '''                modifier = Modifier.fillMaxWidth().padding(16.dp),
'''
field_mod_new = '''                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(searchFocusRequester),
'''
assert field_mod in ss, "Search field modifier not found"
ss = ss.replace(field_mod, field_mod_new, 1)
search.write_text(ss)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Search bottom navigation now uses a two-step tap: first tap opens Search, tapping Search again focuses the search box and opens the keyboard automatically",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
