package com.example.calculadoraimc.ui.opciones

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.calculadoraimc.R
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import com.google.android.material.appbar.MaterialToolbar

class LicencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licences)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_licences)
        toolbar.setNavigationOnClickListener { finish() }

        val tv = findViewById<TextView>(R.id.tv_licences_content)
        val assetName = "LICENCES.txt"
        try {
            val input = assets.open(assetName)
            val reader = BufferedReader(InputStreamReader(input))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line).append('\n')
                line = reader.readLine()
            }
            reader.close()
            tv.text = sb.toString()
        } catch (e: Exception) {
            tv.text = getString(R.string.licences_asset_missing, assetName)
        }
    }
}
