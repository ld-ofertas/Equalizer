package com.ldofertas.equalizer

import android.app.Activity
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : Activity() {
    private var player: MediaPlayer? = null
    private var eq: Equalizer? = null
    private var visualizer: Visualizer? = null
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var powerButton: Button
    private lateinit var muteButton: Button
    private lateinit var meter: VuMeter
    private val sliders = mutableListOf<SeekBar>()
    private val cyan = Color.rgb(0, 220, 255)
    private val purple = Color.rgb(126, 87, 255)
    private val bg = Color.rgb(8, 11, 18)
    private val panel = Color.rgb(20, 26, 38)
    private var effectOn = true
    private var muted = false
    private var volume = .8f
    private var balance = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (::meter.isInitialized && player?.isPlaying != true) meter.setAmplitude(0)
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
        handler.post(tick)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = 1; setPadding(dp(16), dp(14), dp(16), dp(12)); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = 1 }
        val header = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(12)) }
        header.addView(TextView(this).apply { text = "◉"; textSize = 32f; setTextColor(cyan); typeface = Typeface.DEFAULT_BOLD })
        val heading = LinearLayout(this).apply { orientation = 1; setPadding(dp(10), 0, 0, 0) }
        heading.addView(TextView(this).apply { text = "LD EQUALIZER"; textSize = 22f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
        heading.addView(TextView(this).apply { text = "MESA DE SOM DIGITAL"; textSize = 10f; setTextColor(Color.LTGRAY); letterSpacing = .14f })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply { text = "● LIVE"; textSize = 10f; setTextColor(cyan) })
        body.addView(header)

        val music = card()
        music.addView(label("REPRODUÇÃO"))
        status = TextView(this).apply { text = "Escolha um arquivo de áudio"; textSize = 15f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8), 0, dp(8)) }
        music.addView(status)
        val row = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER }
        row.addView(btn("＋ Arquivos", cyan, Color.BLACK) { pickAudio() }, LinearLayout.LayoutParams(0, dp(44), 2f).apply { rightMargin = dp(6) })
        playButton = btn("▶", purple, Color.WHITE) { togglePlayback() }
        row.addView(playButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) })
        row.addView(btn("■", panel, Color.WHITE) { stopPlayback() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        music.addView(row)
        body.addView(music, params())

        val meterCard = card()
        val meterTitle = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        meterTitle.addView(label("VU METER / NÍVEL DE ÁUDIO"), LinearLayout.LayoutParams(0, -2, 1f))
        meterTitle.addView(label("L  ▮  R"))
        meterCard.addView(meterTitle)
        meter = VuMeter()
        meterCard.addView(meter, LinearLayout.LayoutParams(-1, dp(105)).apply { topMargin = dp(10) })
        body.addView(meterCard, params())

        val power = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(panel) }
        val ptext = LinearLayout(this).apply { orientation = 1 }
        ptext.addView(TextView(this).apply { text = "PROCESSADOR DE ÁUDIO"; textSize = 11f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
        ptext.addView(TextView(this).apply { text = "Equalizador de 5 bandas"; textSize = 10f; setTextColor(Color.LTGRAY); setPadding(0, dp(4), 0, 0) })
        power.addView(ptext, LinearLayout.LayoutParams(0, -2, 1f))
        powerButton = btn("⏻ LIGADO", cyan, Color.BLACK) { togglePower() }
        power.addView(powerButton, LinearLayout.LayoutParams(-2, dp(42)))
        body.addView(power, params())

        val presetsCard = card()
        presetsCard.addView(label("PREDEFINIÇÕES"))
        val presetScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val presetRow = LinearLayout(this).apply { orientation = 0; setPadding(0, dp(10), 0, 0) }
        listOf("Normal", "Rock", "Pop", "Jazz", "Clássica", "Dance", "Vocal", "Graves+").forEachIndexed { i, name ->
            presetRow.addView(btn(name, if (i == 0) cyan else Color.rgb(38, 46, 62), if (i == 0) Color.BLACK else Color.WHITE) { applyPreset(i) },
                LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(6) })
        }
        presetScroll.addView(presetRow)
        presetsCard.addView(presetScroll)
        body.addView(presetsCard, params())

        val eqCard = card()
        eqCard.addView(label("EQUALIZADOR"))
        eqCard.addView(TextView(this).apply { text = "−12 dB                              0 dB                              +12 dB"; textSize = 8f; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, 0) })
        val eqRow = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) }
        val freqs = listOf("60\nHz", "230\nHz", "910\nHz", "3.6k\nHz", "14k\nHz")
        for (i in 0..4) {
            val col = LinearLayout(this).apply { orientation = 1; gravity = Gravity.CENTER }
            val value = TextView(this).apply { text = "0 dB"; textSize = 10f; setTextColor(cyan); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
            col.addView(value, params())
            val seek = SeekBar(this).apply { max = 240; progress = 120; rotation = -90f; progressTintList = android.content.res.ColorStateList.valueOf(cyan); thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE) }
            seek.layoutParams = LinearLayout.LayoutParams(dp(126), dp(38)).apply { topMargin = dp(38); bottomMargin = dp(38) }
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val db = (p - 120) / 10
                    value.text = (if (db > 0) "+" else "") + db + " dB"
                    if (fromUser) setBand(i, db)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
            col.addView(seek)
            col.addView(TextView(this).apply { text = freqs[i]; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }, params())
            eqRow.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            sliders.add(seek)
        }
        eqCard.addView(eqRow)
        body.addView(eqCard, params())

        val volumeCard = card()
        volumeCard.addView(label("VOLUME PRINCIPAL"))
        val volRow = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        volRow.addView(label("🔈"))
        val vol = SeekBar(this).apply { max = 100; progress = 80; progressTintList = android.content.res.ColorStateList.valueOf(cyan); thumbTintList = android.content.res.ColorStateList.valueOf(cyan) }
        val volValue = TextView(this).apply { text = "80%"; textSize = 11f; setTextColor(Color.WHITE) }
        volRow.addView(vol, LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
        volRow.addView(volValue)
        volumeCard.addView(volRow)
        vol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { volume = p / 100f; volValue.text = "$p%"; applyVolume() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        volumeCard.addView(TextView(this).apply { text = "BALANÇO ESQUERDA / DIREITA"; textSize = 10f; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, 0) })
        val balRow = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        balRow.addView(label("L"))
        val bal = SeekBar(this).apply { max = 200; progress = 100; progressTintList = android.content.res.ColorStateList.valueOf(purple); thumbTintList = android.content.res.ColorStateList.valueOf(purple) }
        balRow.addView(bal, LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
        balRow.addView(label("R"))
        volumeCard.addView(balRow)
        bal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { balance = (p - 100) / 100f; applyVolume() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        val bottom = LinearLayout(this).apply { orientation = 0; setPadding(0, dp(8), 0, 0) }
        muteButton = btn("♪ MUTE", Color.rgb(43, 51, 68), Color.WHITE) { muted = !muted; muteButton.text = if (muted) "♪ SOM ATIVO" else "♪ MUTE"; applyVolume() }
        bottom.addView(muteButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(6) })
        bottom.addView(btn("↺ RESETAR", Color.rgb(43, 51, 68), Color.WHITE) { applyPreset(0) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        volumeCard.addView(bottom)
        body.addView(volumeCard, params())
        body.addView(TextView(this).apply { text = "O efeito é aplicado ao áudio reproduzido neste app. O controle de outros aplicativos depende do Android e do aparelho."; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.GRAY); setPadding(dp(6), dp(10), dp(6), dp(8)) }, params())

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun setBand(i: Int, db: Int) {
        try { val e = eq ?: return; if (i < e.numberOfBands) { val r = e.bandLevelRange; e.setBandLevel(i.toShort(), (db * 100).coerceIn(r[0].toInt(), r[1].toInt()).toShort()) } } catch (_: Exception) {}
    }

    private fun applyPreset(index: Int) {
        val profiles = listOf(listOf(0,0,0,0,0), listOf(5,3,-1,3,5), listOf(-1,2,4,3,1), listOf(2,1,-2,2,3), listOf(3,1,-1,1,3), listOf(4,2,0,3,4), listOf(-2,0,3,4,2), listOf(8,6,2,0,-1))
        val profile = profiles[index]
        profile.forEachIndexed { i, db -> sliders[i].progress = (120 + db * 10).coerceIn(0, 240) }
        try {
            val e = eq
            if (e != null) for (i in 0 until min(5, e.numberOfBands.toInt())) {
                val range = e.bandLevelRange
                e.setBandLevel(i.toShort(), (profile[i] * 100).coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            status.text = "Preset " + listOf("Normal","Rock","Pop","Jazz","Clássica","Dance","Vocal","Graves+")[index] + " selecionado"
        } catch (_: Exception) { status.text = "Efeito não suportado neste dispositivo" }
    }

    private fun togglePower() {
        effectOn = !effectOn
        try { eq?.enabled = effectOn } catch (_: Exception) {}
        powerButton.text = if (effectOn) "⏻ LIGADO" else "⏻ DESLIGADO"
        powerButton.setBackgroundColor(if (effectOn) cyan else Color.DKGRAY)
        powerButton.setTextColor(if (effectOn) Color.BLACK else Color.WHITE)
    }

    private fun applyVolume() {
        try {
            val p = player ?: return
            val left = if (muted) 0f else volume * if (balance > 0) 1f - balance else 1f
            val right = if (muted) 0f else volume * if (balance < 0) 1f + balance else 1f
            p.setVolume(left.coerceIn(0f, 1f), right.coerceIn(0f, 1f))
        } catch (_: Exception) {}
    }

    private fun pickAudio() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*" }, 100)
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
                    try { eq = Equalizer(0, media.audioSessionId).apply { enabled = effectOn } } catch (_: Exception) { eq = null }
                    setupVisualizer(media.audioSessionId)
                    status.text = "Reproduzindo arquivo selecionado"
                    playButton.text = "Ⅱ"
                    applyVolume()
                    media.start()
                }
                mp.setOnCompletionListener { status.text = "Reprodução concluída"; playButton.text = "▶"; meter.setAmplitude(0) }
                mp.setOnErrorListener { _, _, _ -> status.text = "Erro ao reproduzir áudio"; playButton.text = "▶"; true }
                status.text = "Carregando música…"
                mp.prepareAsync()
            } catch (_: Exception) { status.text = "Não foi possível abrir esse arquivo" }
        }
    }

    private fun setupVisualizer(audioSessionId: Int) {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            val v = Visualizer(audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(effect: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    if (player?.isPlaying != true) return
                    var sum = 0.0
                    for (sample in waveform) {
                        val centered = (sample.toInt() and 0xFF) - 128
                        sum += centered * centered
                    }
                    val rms = if (waveform.isNotEmpty()) sqrt(sum / waveform.size) else 0.0
                    val amplitude = (rms * 256.0).toInt().coerceIn(0, 32768)
                    if (::meter.isInitialized) meter.setAmplitude(amplitude)
                }
                override fun onFftDataCapture(effect: Visualizer, fft: ByteArray, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            v.enabled = true
            visualizer = v
        } catch (_: SecurityException) {
            if (::status.isInitialized) status.text = "Permita o acesso ao microfone para visualizar o ritmo"
        } catch (_: Exception) {
            if (::status.isInitialized) status.text = "VU meter indisponível neste aparelho"
        }
    }

    private fun togglePlayback() {
        val p = player
        if (p == null) { pickAudio(); return }
        try { if (p.isPlaying) { p.pause(); playButton.text = "▶"; status.text = "Pausado" } else { p.start(); playButton.text = "Ⅱ"; status.text = "Reproduzindo música" } } catch (_: Exception) {}
    }

    private fun stopPlayback() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { visualizer?.enabled = false } catch (_: Exception) {}
        try { visualizer?.release() } catch (_: Exception) {}
        visualizer = null
        try { eq?.release() } catch (_: Exception) {}
        eq = null
        if (::status.isInitialized) status.text = "Escolha um arquivo de áudio"
        if (::playButton.isInitialized) playButton.text = "▶"
        if (::meter.isInitialized) meter.setAmplitude(0)
    }

    private fun card() = LinearLayout(this).apply { orientation = 1; setPadding(dp(12), dp(12), dp(12), dp(12)); background = rounded(panel) }
    private fun label(s: String) = TextView(this).apply { text = s; textSize = 11f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; letterSpacing = .05f }
    private fun btn(s: String, color: Int, textColor: Int, action: () -> Unit) = Button(this).apply {
        text = s; textSize = 11f; isAllCaps = false; setTextColor(textColor); background = rounded(color)
        minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0; setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener { action() }
    }
    private fun rounded(color: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(13).toFloat() }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun params() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(9) }

    override fun onDestroy() { handler.removeCallbacks(tick); stopPlayback(); super.onDestroy() }

    private inner class VuMeter : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var level = 0f
        private var peak = 0f
        fun setAmplitude(a: Int) {
            level = (ln(1.0 + a.toDouble()) / ln(32768.0)).toFloat().coerceIn(0f, 1f)
            peak = max(level, peak * .82f)
            invalidate()
        }
        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val gap = dp(3).toFloat()
            val w = (width - gap * 35) / 36f
            val hmax = height.toFloat()
            for (i in 0 until 36) {
                val wave = (sin(i * .63 + System.currentTimeMillis() / 170.0) + 1.0) * .09
                val h = max(dp(4).toFloat(), min(hmax, hmax * peak * (.65 + wave + (i % 5) * .025).toFloat()))
                paint.color = when { i > 29 -> Color.rgb(255, 75, 95); i > 21 -> Color.rgb(255, 190, 65); i % 3 == 0 -> purple; else -> cyan }
                val x = i * (w + gap)
                c.drawRoundRect(RectF(x, hmax - h, x + w, hmax), w / 2, w / 2, paint)
            }
            if (player?.isPlaying == true) postInvalidateDelayed(85)
        }
    }
}
