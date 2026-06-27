package com.example.tuner432

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Usługa grająca w tle. MediaSessionService daje za darmo:
 *  - granie przy zablokowanym ekranie (foreground service + powiadomienie)
 *  - sterowanie z ekranu blokady i powiadomienia
 *  - metadane (tytuł utworu / stacja) na ekran blokady ORAZ na Bluetooth (AVRCP)
 *
 * Pitch 432/440 ustawiony na playerze -> działa też w tle.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val attrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)   // pauza po wyjęciu słuchawek
            .build()

        // Domyślnie konwersja 432 Hz (tempo bez zmian)
        player.playbackParameters = PlaybackParameters(1f, 432f / 440f)

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Gdy użytkownik usunie apkę z listy ostatnich, a nic nie gra -> zamknij usługę
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
