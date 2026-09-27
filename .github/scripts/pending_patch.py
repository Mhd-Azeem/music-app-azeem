from pathlib import Path

search = Path("app/src/main/java/com/wavelength/music/ui/search/SearchScreen.kt")
s = search.read_text()

if "import androidx.compose.runtime.mutableIntStateOf\n" not in s:
    s = s.replace(
        "import androidx.compose.runtime.mutableStateOf\n",
        "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.mutableIntStateOf\n",
        1
    )

old = '''    val context = LocalContext.current
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
new = '''    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Initialize to the current sequence whenever Search enters composition. This prevents an old
    // second-tap request from reopening the IME when the user later returns to Search normally.
    var lastHandledFocusRequest by remember {
        mutableIntStateOf(focusRequestSequence)
    }

    LaunchedEffect(Unit) { viewModel.consumePendingSearch() }
    LaunchedEffect(focusRequestSequence) {
        if (focusRequestSequence > lastHandledFocusRequest) {
            lastHandledFocusRequest = focusRequestSequence
            delay(60)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
'''
assert old in s, "existing search focus effect not found"
s = s.replace(old, new, 1)
search.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Fixed Search keyboard behavior so returning to Search stays keyboard-free; only tapping the already-selected Search tab a second time opens the keyboard",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
