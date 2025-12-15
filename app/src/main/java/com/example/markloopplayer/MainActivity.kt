package com.example.markloopplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import android.content.Intent
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import android.util.Log

/*
Used beginner app creation tutorial https://developer.android.com/codelabs/basic-android-kotlin-compose-first-app#1
    TODO: remove the code from the default app once it's no longer needed
Used some code from https://www.geeksforgeeks.org/android/how-to-build-a-simple-music-player-app-using-android-studio/
Used Claude for Java->Kotlin conversion
Reviewed online documentation
    https://developer.android.com/reference/android/media/MediaPlayer#seekTo(long,%20int)
    https://developer.android.com/media/platform/mediaplayer/basics
    https://stackoverflow.com/questions/2169294/how-to-add-manifest-permission-to-an-application
    https://developer.android.com/reference/android/os/PowerManager
    https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON
Used Claude to identify concurrency concerns
    TODO: fix these identified concurrency issues
Used Claude to explain pathData codes
Used Claude to understand options to hold wake lock
Used Claude to understand how to import files into the app
Used Claude to create pleasant button UI
Changed name from TestApplication1 to MarkLoopPlayer
    https://www.geeksforgeeks.org/android/different-ways-to-change-the-project-name-in-android-studio/
    https://www.reddit.com/r/androiddev/comments/ymphwy/changing_project_name/
    https://stackoverflow.com/questions/18276872/change-project-name-on-android-studio


TODO: test with multiple copies of the same file to verify smoothness of seeking
TODO: test whether jumping to sync frame or directly to millisecond is smoother
TODO: what if the files are different lengths? (maybe use shortest length)
TODO: check for if previously provided file path is now broken, delete and ask for new one
TODO: allow user to delete previously loaded files  

**Looping**
TODO: create drawables for the [ and ] symbols and the same each with a slash through it
TODO: create buttons that will set and unset the beginning and end of the loop
TODO: cause the recording to loop by default
TODO: cause the recording to loop between the specified beginning and end times
TODO: mark on the timeline where the beginning and end of the loop are and the timestamps

**File Buttons**
TODO: When importing or switching recordings start from current play time if possible
TODO: Store reference data to internal file
TODO: Allow user to swipe button to clear data from it

**Other Features**
TODO: Allow user to store multiple banks of related files and swap between them
TODO: Allow user to connect to streaming resources instead of on the phone

**Publishing**
TODO: clean up and put on github
TODO: clean up and put on google play store



 */


class MainActivity : AppCompatActivity() {

    // Declare MediaPlayer for audio playback
    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayer0: MediaPlayer? = null
    private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var mediaPlayer3: MediaPlayer? = null
    private var mediaPlayer4: MediaPlayer? = null
    private var mediaPlayer5: MediaPlayer? = null
    private var mediaPlayer6: MediaPlayer? = null

    // Declare UI elements
    private lateinit var seekBar: SeekBar
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var buttonPlay: ImageView
    private lateinit var buttonPause: ImageView
    private lateinit var buttonStop: ImageView
    private lateinit var buttonImport1: MaterialButton
    private lateinit var buttonImport2: MaterialButton
    private lateinit var buttonImport3: MaterialButton
    private lateinit var buttonImport4: MaterialButton
    private lateinit var buttonImport5: MaterialButton
    private lateinit var buttonImport6: MaterialButton
    private lateinit var buttonImportUri1: Uri
    private lateinit var buttonImportUri2: Uri
    private lateinit var buttonImportUri3: Uri
    private lateinit var buttonImportUri4: Uri
    private lateinit var buttonImportUri5: Uri
    private lateinit var buttonImportUri6: Uri
    private var activeButtonNum: Int = 0

    // Handler to update SeekBar and current time text every second
    private val handler = Handler(Looper.getMainLooper())

    // Runnable task that updates SeekBar and current playback time
    private val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    // Update SeekBar progress and current time text
                    seekBar.progress = player.currentPosition
                    textCurrentTime.text = formatTime(player.currentPosition)

                    // Repeat this task every 1 second
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    // TODO: there has to be a better way to split these up
    private val filePickerLauncher1 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,1) }
    }

    private val filePickerLauncher2 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,2) }
    }

    private val filePickerLauncher3 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,3) }
    }

    private val filePickerLauncher4 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,4) }
    }

    private val filePickerLauncher5 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,5) }
    }

    private val filePickerLauncher6 = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it,6) }
    }

    private fun handleSelectedFile(uri: Uri, buttonNum: Int) {
        // Take persistable permission to access the file later
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        activeButtonNum = buttonNum
        loadAudioFromUri(uri,"start")
        when(buttonNum) {
            1 -> buttonImport1.text = getFileName(uri)
            2 -> buttonImport2.text = getFileName(uri)
            3 -> buttonImport3.text = getFileName(uri)
            4 -> buttonImport4.text = getFileName(uri)
            5 -> buttonImport5.text = getFileName(uri)
            6 -> buttonImport6.text = getFileName(uri)
        }
        when(buttonNum) {
            1 -> buttonImportUri1 = uri
            2 -> buttonImportUri2 = uri
            3 -> buttonImportUri3 = uri
            4 -> buttonImportUri4 = uri
            5 -> buttonImportUri5 = uri
            6 -> buttonImportUri6 = uri
        }
    }

    private fun copyFileToInternalStorage() {
        val fileName = "audio_files_list.txt"
        val outputFile = File(filesDir, fileName)
        // TODO: write out current list of audio files
        // TODO: function to read in prior list of audio files
    }

    private fun getFileName(uri: Uri): String {
        var name = "imported_audio"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    // TODO: move magic strings into enums
    private fun loadAudioFromUri(uri: Uri, mode: String) {
        if(mode=="stop") {
            Log.e("loadAudioFromUri","stop")
            // Stop current playback
            handler.removeCallbacks(updateSeekBar)
            mediaPlayer?.release()

            // Create new MediaPlayer with selected file
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)
                setOnPreparedListener { mp ->
                    seekBar.max = mp.duration
                    seekBar.progress = 0
                    textCurrentTime.text = "0:00"
                    textTotalTime.text = formatTime(mp.duration)
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading file: $what",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                prepareAsync()
            }
            when(activeButtonNum) {
                1 -> mediaPlayer1 = mediaPlayer
                2 -> mediaPlayer2 = mediaPlayer
                3 -> mediaPlayer3 = mediaPlayer
                4 -> mediaPlayer4 = mediaPlayer
                5 -> mediaPlayer5 = mediaPlayer
                6 -> mediaPlayer6 = mediaPlayer
            }
        } else if(mode=="start") {
            Log.e("loadAudioFromUri","start")
            // Stop current playback
            val currentPositionTmp = mediaPlayer?.currentPosition ?: 0
            handler.removeCallbacks(updateSeekBar)
            mediaPlayer?.pause()
            // Create new MediaPlayer with selected file
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)
                setOnPreparedListener { mp ->
                    seekBar.max = mp.duration
                    seekBar.progress = currentPositionTmp
                    // TODO: try different modes for seekTo
                    mp.seekTo(currentPositionTmp)
                    textCurrentTime.text = formatTime(currentPositionTmp)
                    textTotalTime.text = formatTime(mp.duration)
                    mp?.start()
                    handler.post(updateSeekBar)
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading file: $what",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                prepareAsync()
            }
            when(activeButtonNum) {
                1 -> mediaPlayer1 = mediaPlayer
                2 -> mediaPlayer2 = mediaPlayer
                3 -> mediaPlayer3 = mediaPlayer
                4 -> mediaPlayer4 = mediaPlayer
                5 -> mediaPlayer5 = mediaPlayer
                6 -> mediaPlayer6 = mediaPlayer
            }
        } else {
            Log.e("loadAudioFromUri","continue1")
            // mode == "continue"
            // Stop current playback
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer==null))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer0))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer1))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer2))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer3))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer4))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer5))
            Log.e("loadAudioFromUri","continue8" + (mediaPlayer===mediaPlayer6))
            val currentPositionTmp = mediaPlayer?.currentPosition ?: 0
            handler.removeCallbacks(updateSeekBar)
            mediaPlayer?.pause()
            Log.e("loadAudioFromUri","continue2")

            Log.e("loadAudioFromUri","continue81" + (mediaPlayer==null))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer0))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer1))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer2))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer3))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer4))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer5))
            Log.e("loadAudioFromUri","continue81" + (mediaPlayer===mediaPlayer6))
            Log.e("loadAudioFromUri","continue81" + activeButtonNum)
            when(activeButtonNum) {
                1 -> mediaPlayer = mediaPlayer1
                2 -> mediaPlayer = mediaPlayer2
                3 -> mediaPlayer = mediaPlayer3
                4 -> mediaPlayer = mediaPlayer4
                5 -> mediaPlayer = mediaPlayer5
                6 -> mediaPlayer = mediaPlayer6
            }

            Log.e("loadAudioFromUri","continue82" + (mediaPlayer==null))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer0))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer1))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer2))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer3))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer4))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer5))
            Log.e("loadAudioFromUri","continue82" + (mediaPlayer===mediaPlayer6))
            Log.e("loadAudioFromUri","continue3")

/*
            mediaPlayer?.setOnPreparedListener { mp ->
                Log.e("loadAudioFromUri", "continue4")
                seekBar.max = mp.duration
                seekBar.progress = currentPositionTmp
                // TODO: try different modes for seekTo
                mp.seekTo(currentPositionTmp)
                Log.e("loadAudioFromUri", "continue5")
                textCurrentTime.text = formatTime(currentPositionTmp)
                textTotalTime.text = formatTime(mp.duration)
                Log.e("loadAudioFromUri", "continue6")
                mediaPlayer?.start()
                Log.e("loadAudioFromUri", "continue7")
                handler.post(updateSeekBar)
            }*/
            Log.e("loadAudioFromUri", "continue4")
            seekBar.max = mediaPlayer?.duration ?: 0
            seekBar.progress = currentPositionTmp
            // TODO: try different modes for seekTo
            mediaPlayer?.seekTo(currentPositionTmp)
            Log.e("loadAudioFromUri", "continue5")
            textCurrentTime.text = formatTime(currentPositionTmp)
            textTotalTime.text = formatTime(mediaPlayer?.duration ?: 0)
            Log.e("loadAudioFromUri", "continue6")
            mediaPlayer?.start()
            Log.e("loadAudioFromUri", "continue7")
            handler.post(updateSeekBar)
            Log.e("loadAudioFromUri","continue9")

        }
    }

    // Format milliseconds into minutes:seconds format (e.g., 1:05)
    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun stopButtonClickCreateMediaPlayer() {
        when(activeButtonNum) {
            0 -> mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.sample)
            1 -> loadAudioFromUri(buttonImportUri1,"stop")
            2 -> loadAudioFromUri(buttonImportUri2,"stop")
            3 -> loadAudioFromUri(buttonImportUri3,"stop")
            4 -> loadAudioFromUri(buttonImportUri4,"stop")
            5 -> loadAudioFromUri(buttonImportUri5,"stop")
            6 -> loadAudioFromUri(buttonImportUri6,"stop")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set layout for the activity
        setContentView(R.layout.activity_main)

        // Initialize views from layout
        seekBar = findViewById(R.id.seekBar)
        textCurrentTime = findViewById(R.id.textCurrentTime)
        textTotalTime = findViewById(R.id.textTotalTime)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonPause = findViewById(R.id.buttonPause)
        buttonStop = findViewById(R.id.buttonStop)
        buttonImport1 = findViewById(R.id.buttonImport1)
        buttonImport2 = findViewById(R.id.buttonImport2)
        buttonImport3 = findViewById(R.id.buttonImport3)
        buttonImport4 = findViewById(R.id.buttonImport4)
        buttonImport5 = findViewById(R.id.buttonImport5)
        buttonImport6 = findViewById(R.id.buttonImport6)
        buttonImport1.text = "Import Audio File"
        buttonImport2.text = "Import Audio File"
        buttonImport3.text = "Import Audio File"
        buttonImport4.text = "Import Audio File"
        buttonImport5.text = "Import Audio File"
        buttonImport6.text = "Import Audio File"

        // Create MediaPlayer instance with a raw audio resource
        mediaPlayer0 = MediaPlayer.create(this, R.raw.sample)
        mediaPlayer = mediaPlayer0

        // Set listener to configure SeekBar and total time after MediaPlayer is ready
        mediaPlayer?.setOnPreparedListener { mp ->
            seekBar.max = mp.duration
            textTotalTime.text = formatTime(mp.duration)
        }

        // Play button starts the audio and begins updating UI
        buttonPlay.setOnClickListener {
            mediaPlayer?.start()
            handler.post(updateSeekBar)
        }

        // Pause button pauses the audio playback
        buttonPause.setOnClickListener {
            mediaPlayer?.pause()
        }

        // Stop button stops playback and resets UI and MediaPlayer
        buttonStop.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            stopButtonClickCreateMediaPlayer()
            if(activeButtonNum==0) {
                seekBar.progress = 0
                textCurrentTime.text = "0:00"
                mediaPlayer?.let {
                    textTotalTime.text = formatTime(it.duration)
                }
            }
        }

        // Listen for SeekBar user interaction
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            // Called when progress is changed
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Seek MediaPlayer to new position and update current time
                    mediaPlayer?.seekTo(progress)
                    textCurrentTime.text = formatTime(progress)
                }
            }

            // Not used, but required to override
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            // Not used, but required to override
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        buttonImport1.setOnClickListener {
            if(::buttonImportUri1.isInitialized) {
                if(activeButtonNum!=1) {
                    activeButtonNum = 1
                    loadAudioFromUri(buttonImportUri1, "continue")
                }
            } else {
                filePickerLauncher1.launch(arrayOf("audio/*"))
            }
        }
        buttonImport2.setOnClickListener {
            if(::buttonImportUri2.isInitialized) {
                if(activeButtonNum!=2) {
                    activeButtonNum = 2
                    loadAudioFromUri(buttonImportUri2, "continue")
                }
            } else {
                filePickerLauncher2.launch(arrayOf("audio/*"))
            }
        }
        buttonImport3.setOnClickListener {
            if(::buttonImportUri3.isInitialized) {
                if(activeButtonNum!=3) {
                    activeButtonNum = 3
                    loadAudioFromUri(buttonImportUri3, "continue")
                }
            } else {
                filePickerLauncher3.launch(arrayOf("audio/*"))
            }
        }
        buttonImport4.setOnClickListener {
            if(::buttonImportUri4.isInitialized) {
                if(activeButtonNum!=4) {
                    activeButtonNum = 4
                    loadAudioFromUri(buttonImportUri4, "continue")
                }
            } else {
                filePickerLauncher4.launch(arrayOf("audio/*"))
            }
        }
        buttonImport5.setOnClickListener {
            if(::buttonImportUri5.isInitialized) {
                if(activeButtonNum!=5) {
                    activeButtonNum = 5
                    loadAudioFromUri(buttonImportUri5, "continue")
                }
            } else {
                filePickerLauncher5.launch(arrayOf("audio/*"))
            }
        }
        buttonImport6.setOnClickListener {
            if(::buttonImportUri6.isInitialized) {
                if(activeButtonNum!=6) {
                    activeButtonNum = 6
                    loadAudioFromUri(buttonImportUri6, "continue")
                }
            } else {
                filePickerLauncher6.launch(arrayOf("audio/*"))
            }
        }
    }

    // Clean up MediaPlayer and handler when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}