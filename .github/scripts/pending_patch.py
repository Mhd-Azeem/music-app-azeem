from pathlib import Path

path = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str) -> None:
    global s
    if old not in s:
        raise SystemExit(f'Expected Glass Now Playing source pattern not found:\n{old}')
    s = s.replace(old, new, 1)


if 'import com.wavelength.music.ui.design.UiDesignConfig\n' not in s:
    replace_once(
        'import com.wavelength.music.ui.components.swipeHorizontal\n',
        'import com.wavelength.music.ui.components.swipeHorizontal\nimport com.wavelength.music.ui.design.UiDesignConfig\n'
    )

replace_once(
    '''                            modifier = Modifier.fillMaxWidth().height(214.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp),''',
    '''                            modifier = Modifier.fillMaxWidth().height(UiDesignConfig.GLASS_QUEUE_AREA_HEIGHT_DP.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(UiDesignConfig.GLASS_QUEUE_GAP_DP.dp),'''
)
replace_once(
    '                                val lift = with(LocalDensity.current) { (circleY * 58f).dp }',
    '                                val lift = with(LocalDensity.current) { (circleY * UiDesignConfig.GLASS_QUEUE_ARC_DEPTH_DP).dp }'
)
replace_once(
    '''                                        .width(82.dp)
                                        .height(176.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = tangentAngle }
                                        .clip(RoundedCornerShape(11.dp))''',
    '''                                        .width(UiDesignConfig.GLASS_QUEUE_CARD_WIDTH_DP.dp)
                                        .height(UiDesignConfig.GLASS_QUEUE_CARD_HEIGHT_DP.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = tangentAngle }
                                        .clip(RoundedCornerShape(UiDesignConfig.GLASS_QUEUE_CARD_RADIUS_DP.dp))'''
)
replace_once(
    '''                        .height(356.dp)
                        .offset(y = (-2).dp)''',
    '''                        .height(UiDesignConfig.GLASS_LOWER_PANEL_HEIGHT_DP.dp)
                        .offset(y = (-2).dp)'''
)
replace_once(
    '                        modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 0.dp, end = 0.dp, bottom = 0.dp),',
    '                        modifier = Modifier.fillMaxSize().padding(top = UiDesignConfig.GLASS_LOWER_PANEL_TOP_PADDING_DP.dp, start = 0.dp, end = 0.dp, bottom = 0.dp),'
)
replace_once(
    '''                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .height(92.dp)''',
    '''                                .fillMaxWidth()
                                .padding(horizontal = UiDesignConfig.GLASS_SEEK_HORIZONTAL_PADDING_DP.dp)
                                .height(92.dp)'''
)
replace_once(
    '                                    style = Stroke(width = 3.2.dp.toPx())',
    '                                    style = Stroke(width = UiDesignConfig.GLASS_SEEK_STROKE_DP.dp.toPx())'
)
replace_once(
    '                                drawCircle(Color(0xFF00A9D6), 7.dp.toPx(), thumb)',
    '                                drawCircle(Color(0xFF00A9D6), UiDesignConfig.GLASS_SEEK_THUMB_RADIUS_DP.dp.toPx(), thumb)'
)
replace_once(
    '''                                    .width(322.dp)
                                    .height(184.dp)''',
    '''                                    .width(UiDesignConfig.GLASS_TRANSPORT_FRAME_WIDTH_DP.dp)
                                    .height(UiDesignConfig.GLASS_TRANSPORT_FRAME_HEIGHT_DP.dp)'''
)
replace_once(
    '''                                        .width(292.dp)
                                        .height(142.dp)''',
    '''                                        .width(UiDesignConfig.GLASS_TRANSPORT_BODY_WIDTH_DP.dp)
                                        .height(UiDesignConfig.GLASS_TRANSPORT_BODY_HEIGHT_DP.dp)'''
)
replace_once(
    '''                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(56.dp)''',
    '''                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)'''
)
replace_once(
    '''                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(56.dp)''',
    '''                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)'''
)
replace_once(
    '''                                        .padding(start = 31.dp)
                                        .offset(y = 18.dp)
                                        .size(68.dp)''',
    '''                                        .padding(start = 31.dp)
                                        .offset(y = 18.dp)
                                        .size(UiDesignConfig.GLASS_SIDE_CONTROL_SIZE_DP.dp)'''
)
replace_once(
    '''                                        .padding(end = 31.dp)
                                        .offset(y = 18.dp)
                                        .size(68.dp)''',
    '''                                        .padding(end = 31.dp)
                                        .offset(y = 18.dp)
                                        .size(UiDesignConfig.GLASS_SIDE_CONTROL_SIZE_DP.dp)'''
)
replace_once(
    '''                                        .padding(bottom = 11.dp)
                                        .size(120.dp)''',
    '''                                        .padding(bottom = 11.dp)
                                        .size(UiDesignConfig.GLASS_PLAY_OUTER_SIZE_DP.dp)'''
)
replace_once(
    '''                                            .size(94.dp)
                                            .clip(CircleShape)''',
    '''                                            .size(UiDesignConfig.GLASS_PLAY_INNER_SIZE_DP.dp)
                                            .clip(CircleShape)'''
)

path.write_text(s, encoding='utf-8')
print('AzMusic Glass Now Playing designer bindings applied.')
