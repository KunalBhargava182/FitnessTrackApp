package com.example.fittrackapp

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isSensorPresent = false

    private lateinit var tvSteps: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvCalories: TextView
    private lateinit var lineChart: LineChart

    private var totalSteps = 0f
    private var previousSteps = 0f
    private var entryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSteps = findViewById(R.id.tvSteps)
        tvDistance = findViewById(R.id.tvDistance)
        tvCalories = findViewById(R.id.tvCalories)
        lineChart = findViewById(R.id.lineChart)

        setupChart()
        checkAndRequestActivityRecognitionPermission()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            isSensorPresent = true
        } else {
            tvSteps.text = "Step Counter Sensor Not Found!"
            isSensorPresent = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSensorPresent) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSensorPresent) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = event.values[0]

            val currentSteps = totalSteps - previousSteps
            if (previousSteps == 0f) previousSteps = totalSteps

            tvSteps.text = "Steps: ${currentSteps.toInt()}"

            val distance = (currentSteps * 0.0008).toFloat()  // 0.8 meters per step
            val calories = (currentSteps * 0.04).toFloat()    // 0.04 kcal per step

            tvDistance.text = "Distance: %.2f km".format(distance)
            tvCalories.text = "Calories: %.2f kcal".format(calories)

            updateChart(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        val dataSet = LineDataSet(mutableListOf(), "Steps")
        dataSet.color = resources.getColor(R.color.black, theme)
        dataSet.valueTextColor = resources.getColor(R.color.black, theme)
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)

        lineChart.data = LineData(dataSet)

        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    }

    private fun updateChart(steps: Float) {
        val data = lineChart.data
        val dataSet = data.getDataSetByIndex(0)

        dataSet.addEntry(Entry(entryIndex++.toFloat(), steps))
        data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    1001
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission required for step tracking", Toast.LENGTH_LONG).show()
            }
        }
    }
}
