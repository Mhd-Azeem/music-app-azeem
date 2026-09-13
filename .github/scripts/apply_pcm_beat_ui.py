from pathlib import Path

path = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
text = path.read_text()
start_marker = '    // Scale is animated only when the live FFT detector below reports a real audio onset.\n'
end_marker = '    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()\n'
start = text.index(start_marker)
end = text.index(end_marker, start)
replacement = '''    // Beat Bounce is driven directly by decoded PCM from ExoPlayer's audio processor chain.
    // No Visualizer callback, microphone permission, fixed timer or synthetic BPM clock is used.
    val beatBounceScale = remember { Animatable(1f) }
    val pcmBeatPulse by viewModel.pcmBeatPulse.collectAsStateWithLifecycle()
    var lastHandledPcmBeat by remember { mutableStateOf(0L) }

    LaunchedEffect(beatBounceAlbumArt) {
        if (beatBounceAlbumArt) {
            // Do not replay an old beat merely because the user just enabled the effect.
            lastHandledPcmBeat = pcmBeatPulse.sequence
        } else {
            beatBounceScale.animateTo(1f, animationSpec = tween(120))
        }
    }

    LaunchedEffect(pcmBeatPulse.sequence, beatBounceAlbumArt, state.isPlaying) {
        if (
            beatBounceAlbumArt &&
            state.isPlaying &&
            pcmBeatPulse.sequence > lastHandledPcmBeat
        ) {
            lastHandledPcmBeat = pcmBeatPulse.sequence
            val amount = pcmBeatPulse.strength.coerceIn(0.055f, 0.16f)
            beatBounceScale.stop()
            beatBounceScale.snapTo(1f)
            beatBounceScale.animateTo(
                1f + amount,
                animationSpec = tween(durationMillis = 58, easing = LinearEasing)
            )
            beatBounceScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }
    }

    // RECORD_AUDIO is now needed only for the optional waveform visualizer UI. Beat Bounce no
    // longer depends on Android's Visualizer API or any runtime audio-recording permission.
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordAudioPermission = granted }
    val needsAudioCapture = audioVisualizerEnabled
    LaunchedEffect(needsAudioCapture, hasRecordAudioPermission) {
        if (needsAudioCapture && !hasRecordAudioPermission) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val visualizerWaveform by viewModel.visualizerWaveform.collectAsStateWithLifecycle()
    DisposableEffect(needsAudioCapture, hasRecordAudioPermission) {
        viewModel.setVisualizerCaptureEnabled(needsAudioCapture && hasRecordAudioPermission)
        onDispose { viewModel.setVisualizerCaptureEnabled(false) }
    }

'''
path.write_text(text[:start] + replacement + text[end:])
