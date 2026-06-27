package com.example.tuner432

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private var controller: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private lateinit var modeFiles: Button
    private lateinit var modeRadio: Button
    private lateinit var pickButton: Button
    private lateinit var stationSpinner: Spinner
    private lateinit var playButton: Button
    private lateinit var switch432: Switch
    private lateinit var status: TextView
    private lateinit var nowPlaying: TextView
    private lateinit var nowStation: TextView

    private var pickedFile: Uri? = null
    private var pickedFileName: String? = null
    private var radioMode = false
    private var convert432 = true
    private var currentLabel = "\u2014"     // stacja lub plik (gdy brak metadanych)

    private val pickInput = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedFile = uri
            pickedFileName = fileName(uri) ?: "Wybrany plik"
            currentLabel = pickedFileName!!
            nowPlaying.text = currentLabel
            nowStation.visibility = View.GONE
            status.text = "Plik gotowy. Wci\u015Bnij \u25B6"
        }
    }

    private val askNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* bez powiadomień gra dalej, ale bez sterowania z blokady */ }

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(md: MediaMetadata) {
            val title = md.title?.toString()?.takeIf { it.isNotBlank() }
            if (title != null && title != currentLabel) {
                nowPlaying.text = title          // tytuł utworu z ICY
                nowStation.text = currentLabel   // pod spodem nazwa stacji
                nowStation.visibility = View.VISIBLE
            } else {
                nowPlaying.text = currentLabel
                nowStation.visibility = View.GONE
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateTransport()
        override fun onPlaybackStateChanged(state: Int) = updateTransport()
        override fun onPlayerError(error: PlaybackException) {
            playButton.text = "\u25B6"
            status.text = "Stacja niedost\u0119pna (${error.errorCodeName})"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        modeFiles = findViewById(R.id.modeFiles)
        modeRadio = findViewById(R.id.modeRadio)
        pickButton = findViewById(R.id.pickButton)
        stationSpinner = findViewById(R.id.stationSpinner)
        playButton = findViewById(R.id.playButton)
        switch432 = findViewById(R.id.switch432)
        status = findViewById(R.id.status)
        nowPlaying = findViewById(R.id.nowPlaying)
        nowStation = findViewById(R.id.nowStation)

        val adapter = ArrayAdapter(
            this, R.layout.spinner_selected, RadioStations.list.map { it.name }
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown)
        stationSpinner.adapter = adapter

        stationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (radioMode) {
                    currentLabel = RadioStations.list[pos].name
                    nowPlaying.text = currentLabel
                    nowStation.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        modeFiles.setOnClickListener { setMode(radio = false) }
        modeRadio.setOnClickListener { setMode(radio = true) }
        pickButton.setOnClickListener { pickInput.launch(arrayOf("audio/*")) }
        playButton.setOnClickListener { onPlayClicked() }

        switch432.isChecked = true
        switch432.setOnCheckedChangeListener { _, checked ->
            convert432 = checked
            controller?.playbackParameters =
                PlaybackParameters(1f, if (checked) 432f / 440f else 1f)
            updateTransport()
        }

        // Android 13+: zgoda na powiadomienie (sterowanie z ekranu blokady)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setMode(radio = false)
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(playerListener)
            convert432 = c.playbackParameters.pitch < 0.999f
            switch432.isChecked = convert432
            val t = c.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
            if (t != null) nowPlaying.text = t
            updateTransport()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onStop()
    }

    private fun onPlayClicked() {
        val c = controller ?: run { status.text = "\u0141\u0105cz\u0119 z odtwarzaczem\u2026"; return }

        if (c.isPlaying || c.playbackState == Player.STATE_BUFFERING) {
            c.stop()
            playButton.text = "\u25B6"
            status.text = "Zatrzymano."
            return
        }

        val item: MediaItem = if (radioMode) {
            val st = RadioStations.list[stationSpinner.selectedItemPosition]
            currentLabel = st.name
            MediaItem.Builder().setUri(st.url)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(st.name).setStation(st.name).build()
                ).build()
        } else {
            val uri = pickedFile ?: run { status.text = "Najpierw wybierz plik."; return }
            currentLabel = pickedFileName ?: "Plik"
            MediaItem.Builder().setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(currentLabel).build())
                .build()
        }

        nowPlaying.text = currentLabel
        nowStation.visibility = View.GONE
        c.setMediaItem(item)
        c.prepare()
        c.play()
        playButton.text = "\u25A0"
        status.text = "Buforowanie\u2026"
    }

    private fun updateTransport() {
        val c = controller ?: return
        when {
            c.playbackState == Player.STATE_BUFFERING -> {
                status.text = "Buforowanie\u2026"; playButton.text = "\u25A0"
            }
            c.isPlaying -> {
                status.text = if (convert432) "Gra @ 432 Hz" else "Gra @ 440 Hz (oryginał)"
                playButton.text = "\u25A0"
            }
            c.playbackState == Player.STATE_ENDED -> {
                status.text = "Koniec."; playButton.text = "\u25B6"
            }
            else -> playButton.text = "\u25B6"
        }
    }

    private fun setMode(radio: Boolean) {
        radioMode = radio
        if (radio) {
            pickButton.visibility = View.GONE
            stationSpinner.visibility = View.VISIBLE
            modeRadio.setBackgroundResource(R.drawable.pill_active)
            modeRadio.setTextColor(0xFF1A1024.toInt())
            modeFiles.setBackgroundResource(R.drawable.pill_inactive)
            modeFiles.setTextColor(0xFFE7C77B.toInt())
            val pos = stationSpinner.selectedItemPosition.coerceAtLeast(0)
            currentLabel = RadioStations.list[pos].name
            status.text = "Tryb RADIO. Wybierz stacj\u0119 i \u25B6"
        } else {
            pickButton.visibility = View.VISIBLE
            stationSpinner.visibility = View.GONE
            modeFiles.setBackgroundResource(R.drawable.pill_active)
            modeFiles.setTextColor(0xFF1A1024.toInt())
            modeRadio.setBackgroundResource(R.drawable.pill_inactive)
            modeRadio.setTextColor(0xFFE7C77B.toInt())
            currentLabel = pickedFileName ?: "\u2014"
            status.text = "Tryb PLIKI. Wybierz plik i \u25B6"
        }
        nowPlaying.text = currentLabel
        nowStation.visibility = View.GONE
        controller?.stop()
        playButton.text = "\u25B6"
    }

    private fun fileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
        }
        return name
    }
}
