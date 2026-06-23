package com.bastiankahuna.karoosmartftp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.bastiankahuna.karoosmartftp.engine.Formatters
import com.bastiankahuna.karoosmartftp.model.WorkoutDefinition
import com.bastiankahuna.karoosmartftp.model.WorkoutLibrary
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var store: SettingsStore
    private lateinit var workoutSpinner: Spinner
    private lateinit var ftpInput: EditText
    private lateinit var useSystemFtp: CheckBox
    private lateinit var details: TextView
    private lateinit var history: TextView

    private val cyan = Color.rgb(0, 229, 255)
    private val amber = Color.rgb(255, 176, 0)
    private val white = Color.rgb(232, 255, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        setContentView(buildView())
        bindValues()
    }

    private fun buildView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(5, 7, 11))
        }

        root.addView(TextView(this).apply {
            text = "SMART FTP FIELD v1.1"
            setTextColor(cyan)
            textSize = 26f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        })

        root.addView(label("Training auswählen"))
        workoutSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                WorkoutLibrary.all.map { it.name }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        root.addView(workoutSpinner)

        details = bodyText("")
        root.addView(details)

        useSystemFtp = CheckBox(this).apply {
            text = "FTP aus Karoo-Profil verwenden, falls vorhanden"
            setTextColor(white)
            textSize = 16f
        }
        root.addView(useSystemFtp)

        root.addView(label("Manuelle FTP / Fallback"))
        ftpInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(amber)
            setHintTextColor(Color.DKGRAY)
            textSize = 22f
            gravity = Gravity.CENTER
            setSingleLine(true)
            hint = "250"
        }
        root.addView(ftpInput)

        val saveButton = Button(this).apply {
            text = "Training speichern"
            setOnClickListener { saveOnly() }
        }
        root.addView(saveButton)

        val resetButton = Button(this).apply {
            text = "Training starten / Reset"
            setOnClickListener {
                saveOnly()
                store.resetWorkout()
                Toast.makeText(this@MainActivity, "Training zurückgesetzt. Füge das Datenfeld im Ride-Profil hinzu oder starte die Fahrt.", Toast.LENGTH_LONG).show()
            }
        }
        root.addView(resetButton)

        val skipButton = Button(this).apply {
            text = "Nächstes Segment erzwingen"
            setOnClickListener {
                store.skipRequestedSegment()
                Toast.makeText(this@MainActivity, "Segmentwechsel angefordert.", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(skipButton)

        root.addView(bodyText("Bedienung: Auf dem Karoo eine Ride-Seite mit einem großen Feld anlegen und das Datenfeld ‘Smart FTP Workout’ auswählen. Work-Intervalle zählen nur, wenn 3s Power mindestens Ziel -5% erreicht. Warm-up, Recovery und Cool-down laufen normal."))

        history = bodyText("")
        root.addView(history)

        workoutSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateDetails(WorkoutLibrary.all[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        })

        return ScrollView(this).apply { addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
    }

    private fun bindValues() {
        val selectedIndex = WorkoutLibrary.all.indexOfFirst { it.id == store.selectedWorkoutId }.coerceAtLeast(0)
        workoutSpinner.setSelection(selectedIndex)
        useSystemFtp.isChecked = store.useSystemFtp
        ftpInput.setText(store.manualFtp.toString())
        updateDetails(WorkoutLibrary.all[selectedIndex])
        updateHistory()
    }

    private fun saveOnly() {
        val selected = WorkoutLibrary.all[workoutSpinner.selectedItemPosition.coerceIn(0, WorkoutLibrary.all.lastIndex)]
        val ftp = ftpInput.text.toString().toIntOrNull()?.coerceIn(50, 700) ?: 250
        store.selectedWorkoutId = selected.id
        store.manualFtp = ftp
        store.useSystemFtp = useSystemFtp.isChecked
        updateHistory()
        Toast.makeText(this, "Gespeichert: ${selected.name}, FTP-Fallback $ftp W", Toast.LENGTH_SHORT).show()
    }

    private fun updateDetails(workout: WorkoutDefinition) {
        val duration = Formatters.durationShort(workout.totalDurationSec)
        val work = Formatters.durationShort(workout.workDurationSec)
        details.text = buildString {
            append(workout.description).append('\n')
            append("Dauer: ").append(duration).append("   Work netto: ").append(work).append('\n')
            append("Tags: ").append(workout.tags.joinToString(" / ")).append('\n')
            append("Segmente:\n")
            workout.segments.forEachIndexed { index, s ->
                append(index + 1).append(". ")
                append(s.title).append("  ")
                append(Formatters.durationShort(s.durationSec)).append("  @").append(s.targetPct).append("% FTP")
                if (s.cadenceLow != null && s.cadenceHigh != null) append("  ").append(s.cadenceLow).append("–").append(s.cadenceHigh).append(" rpm")
                append('\n')
            }
        }
    }

    private fun updateHistory() {
        val last = store.lastCompletedAt.takeIf { it > 0L }?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "noch kein Abschluss"
        history.text = "Lokale Historie: ${store.completedCount} abgeschlossene Trainings. Letzter Abschluss: $last"
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(cyan)
        textSize = 18f
        setPadding(0, 28, 0, 8)
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private fun bodyText(textValue: String) = TextView(this).apply {
        text = textValue
        setTextColor(white)
        textSize = 15f
        setPadding(0, 12, 0, 12)
        typeface = android.graphics.Typeface.MONOSPACE
    }
}
