package com.ldofertas.equalizer

import android.app.Activity
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Typeface
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private var player: MediaPlayer? = null
    private var eq: Equalizer? = null
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private val bands = mutableListOf<SeekBar>()
    private val presets = listOf("Personalizado", "Normal", "Rock", "Pop", "Jazz", "Clássica", "Dance", "Vocal")
    private val accent = Color.rgb(91, 100, 255)
    private val bg = Color.rgb(13, 16, 31)
    private val panel = Color.rgb(25, 30, 52)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 28, 22, 20); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply { text = "EQUALIZER"; textSize = 28f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }
        content.addView(title, matchWrap())
        content.addView(TextView(this).apply { text = "Deixe seu som do seu jeito"; textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 20) }, matchWrap())
        status = TextView(this).apply { text = "Escolha uma música para começar"; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; setPadding(12, 14, 12, 14); setBackgroundColor(panel) }
        content.addView(status, matchWrap())
        playButton = Button(this).apply { text = "♪  ESCOLHER MÚSICA"; setOnClickListener { pickAudio() } }
        content.addView(playButton, matchWrap())
        val playRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val play = Button(this).apply { text = "▶  Reproduzir"; setOnClickListener { togglePlayback() } }
        val stop = Button(this).apply { text = "■  Parar"; setOnClickListener { stopPlayback() } }
        playRow.addView(play, LinearLayout.LayoutParams(0, -2, 1f)); playRow.addView(stop, LinearLayout.LayoutParams(0, -2, 1f)); content.addView(playRow, matchWrap())
        content.addView(TextView(this).apply { text = "PREDEFINIÇÕES"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.LTGRAY); setPadding(0, 20, 0, 8) }, matchWrap())
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, presets); onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}; override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { applyPreset(position) } } }
        content.addView(spinner, matchWrap())
        content.addView(TextView(this).apply { text = "EQUALIZAÇÃO DE 5 BANDAS"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.LTGRAY); setPadding(0, 20, 0, 8) }, matchWrap())
        val equalizerPanel = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(4, 12, 4, 12); setBackgroundColor(panel) }
        val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3,6 kHz", "14 kHz")
        for (i in 0 until 5) {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
            val value = TextView(this).apply { text = "0 dB"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
            val seek = SeekBar(this).apply { max = 1200; progress = 600; rotation = -90f; progressTintList = android.content.res.ColorStateList.valueOf(accent); thumbTintList = android.content.res.ColorStateList.valueOf(accent) }
            seek.layoutParams = LinearLayout.LayoutParams(180, 48).apply { topMargin = 55; bottomMargin = 55 }
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { val db = (p - 600) / 100; value.text = "$db dB"; if (fromUser) { try { val range = eq?.bandLevelRange; if (range != null && i < (eq?.numberOfBands ?: 0)) { val level = (range[0] + (p * (range[1] - range[0]) / 1200)).toShort(); eq?.setBandLevel(i.toShort(), level) } } catch (_: Exception) {} } }; override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {} })
            col.addView(value, matchWrap()); col.addView(seek); col.addView(TextView(this).apply { text = labels[i]; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY) }, matchWrap())
            equalizerPanel.addView(col, LinearLayout.LayoutParams(0, -2, 1f)); bands.add(seek)
        }
        content.addView(equalizerPanel, matchWrap())
        val reset = Button(this).apply { text = "REDEFINIR EQUALIZADOR"; setOnClickListener { bands.forEach { it.progress = 600 }; applyPreset(1) } }
        content.addView(reset, matchWrap())
        content.addView(TextView(this).apply { text = "O efeito é aplicado à música reproduzida neste app. O áudio de outros aplicativos pode não ser afetado."; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.GRAY); setPadding(4, 18, 4, 10) }, matchWrap())
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }

    private fun pickAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*" }
        startActivityForResult(intent, 100)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                stopPlayback()
                val mp = MediaPlayer()
                mp.setDataSource(this, uri)
                mp.setOnPreparedListener { media ->
                    player = media
                    try { eq?.release(); eq = Equalizer(0, media.audioSessionId).apply { enabled = true } } catch (_: Exception) { eq = null }
                    status.text = "Música pronta para reproduzir"
                    playButton.text = "♪  Trocar música"
                    media.start()
                }
                mp.setOnCompletionListener { status.text = "Reprodução concluída" }
                mp.setOnErrorListener { _, _, _ -> status.text = "Não foi possível reproduzir este áudio"; true }
                status.text = "Carregando música…"
                mp.prepareAsync()
            } catch (_: Exception) { status.text = "Erro ao abrir a música. Tente outro arquivo." }
        }
    }

    private fun togglePlayback() {
        val p = player
        if (p == null) { pickAudio(); return }
        try { if (p.isPlaying) { p.pause(); status.text = "Reprodução pausada" } else { p.start(); status.text = "Reproduzindo música" } } catch (_: Exception) { status.text = "Não foi possível controlar a reprodução" }
    }

    private fun stopPlayback() { try { player?.stop() } catch (_: Exception) {}; try { player?.release() } catch (_: Exception) {}; player = null; try { eq?.release() } catch (_: Exception) {}; eq = null; if (::status.isInitialized) status.text = "Escolha uma música para começar" }

    private fun applyPreset(position: Int) {
        val p = player ?: return
        try {
            val effect = eq ?: Equalizer(0, p.audioSessionId).also { eq = it; it.enabled = true }
            if (position == 0 || position == 1) { for (i in 0 until minOf(5, effect.numberOfBands.toInt())) effect.setBandLevel(i.toShort(), 0); bands.forEach { it.progress = 600 }; return }
            val profiles = mapOf(2 to listOf(5, 3, -1, 3, 5), 3 to listOf(-1, 2, 4, 3, 1), 4 to listOf(2, 1, -2, 2, 3), 5 to listOf(3, 1, -1, 1, 3), 6 to listOf(4, 2, 0, 3, 4), 7 to listOf(-2, 0, 3, 4, 2))
            val profile = profiles[position] ?: return
            for (i in 0 until minOf(profile.size, effect.numberOfBands.toInt())) {
                val range = effect.bandLevelRange
                val level = (profile[i] * 100).coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                effect.setBandLevel(i.toShort(), level)
                if (i < bands.size) bands[i].progress = (600 + profile[i] * 100).coerceIn(0, 1200)
            }
        } catch (_: Exception) { status.text = "Este dispositivo não oferece suporte a este efeito de áudio" }
    }

    override fun onDestroy() { stopPlayback(); super.onDestroy() }
}
