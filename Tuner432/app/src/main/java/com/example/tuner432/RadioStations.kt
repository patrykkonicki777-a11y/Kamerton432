package com.example.tuner432

/**
 * Stacja radiowa: nazwa + BEZPOŚREDNI adres strumienia (mp3/aac/m3u8).
 * NIE .pls / .m3u — ExoPlayer nie rozwija playlist.
 *
 * Adresy zweryfikowane względem wykazu EMSoft (akt. 06.2026):
 *   - Polskie Radio: bezpośrednie HLS (.m3u8) -> pewne
 *   - ESKA Toruń: potwierdzone (ID 2150)
 *   - RMF: strumienie bezpośrednie (rmfon / rmfstream)
 *   - Eurozet: najlepsze typy "tunein.mp3" -> SPRAWDŹ na telefonie
 *
 * Martwą stację apka oznaczy jako "Stacja niedostępna".
 * Aktualne adresy: http://www.emsoft.ct8.pl/strumienie.php
 */
data class RadioStation(val name: String, val url: String)

object RadioStations {

    val list: List<RadioStation> = listOf(
        // --- RMF (strumienie bezpośrednie) ---
        RadioStation("RMF FM", "http://217.74.72.3:8000/rmf_fm"),
        RadioStation("RMF MAXXX", "http://217.74.72.3:8000/rmf_maxxx"),
        RadioStation("RMF Classic", "http://rs201-krk.rmfstream.pl/rmf_classic_waw"),

        // --- Eurozet (typy do weryfikacji na telefonie) ---
        RadioStation("Radio ZET", "https://radiozet-live.cdn.eurozet.pl/radiozet-tunein.mp3"),
        RadioStation("Antyradio", "https://antyradio-live.cdn.eurozet.pl/antyradio-tunein.mp3"),
        RadioStation("Chillizet", "https://chillizet-live.cdn.eurozet.pl/chillizet-tunein.mp3"),
        RadioStation("Meloradio", "https://meloradio-live.cdn.eurozet.pl/meloradio-tunein.mp3"),

        // --- Grupa Time / Eska (smcdn) ---
        RadioStation("ESKA Toru\u0144", "https://ic1.smcdn.pl/2150-1.mp3"),
        RadioStation("Radio Eska", "https://ic1.smcdn.pl/2380-1.mp3"),
        RadioStation("VOX FM", "https://ic1.smcdn.pl/3990-1.mp3"),

        // --- Polskie Radio (HLS, pewne) ---
        RadioStation("Polskie Radio Tr\u00F3jka", "https://stream13.polskieradio.pl/pr3/pr3.sdp/playlist.m3u8"),
        RadioStation("Polskie Radio Jedynka", "https://stream11.polskieradio.pl/pr1/pr1.sdp/playlist.m3u8"),
        RadioStation("Polskie Radio Dw\u00F3jka", "https://stream12.polskieradio.pl/pr2/pr2.sdp/playlist.m3u8"),
    )
}
