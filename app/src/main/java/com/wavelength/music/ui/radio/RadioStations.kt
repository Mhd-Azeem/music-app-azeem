package com.wavelength.music.ui.radio

import com.wavelength.music.R

internal data class Station(
    val id: String,
    val name: String,
    val details: String,
    val livePage: String,
    val stream: String,
    val cover: Int
)

internal val stations = listOf(
    Station("shakthi", "Shakthi FM", "Tamil · 103.9 / 104.1 FM", "https://shakthifm.com/home.aspx", "https://mbc.thestreamtech.com:8086/stream", R.drawable.fm_shakthi),
    Station("sooriyan", "Sooriyan FM", "Tamil · 103.4 / 103.6 FM", "https://www.sooriyanfm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:8006/", R.drawable.fm_sooriyan),
    Station("vasantham", "Vasantham FM", "Tamil · 102.6 / 102.8 FM", "https://www.vasanthamfm.lk/", "https://cp12.serverse.com/proxy/vasanthamfm?mp=/stream", R.drawable.fm_vasantham),
    Station("slbc-tamil", "SLBC Tamil National", "Tamil · 102.1 / 102.3 FM", "https://slbc.lk/", "https://stream-32.zeno.fm/xekhrn4zetzuv", R.drawable.fm_slbc_tamil),
    Station("hiru", "Hiru FM", "Sinhala · 96.1 / 96.3 FM", "https://www.hirufm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:2020/stream/hirufmgarden/stream/1/", R.drawable.fm_hiru),
    Station("shaa", "Shaa FM", "Sinhala · 90.9 / 91.1 FM", "https://www.shaafm.lk/listenlive.php", "https://radio.lotustechnologieslk.net:2020/stream/shaafmgarden", R.drawable.fm_shaa),
    Station("neth", "Neth FM", "Sinhala · 94.8 / 95.0 FM", "https://nethfm.com/", "https://cp11.serverse.com/proxy/nethfm/stream", R.drawable.fm_neth),
    Station("gold", "Gold FM", "English · 93.0 / 93.2 FM", "https://www.goldfm.lk/", "https://radio.lotustechnologieslk.net:2020/stream/goldfmgarden", R.drawable.fm_gold),
    Station("sun", "Sun FM", "English · 98.7 / 98.9 FM", "https://www.sunfm.lk/", "https://radio.lotustechnologieslk.net:2020/stream/sunfmgarden", R.drawable.fm_sun)
)
