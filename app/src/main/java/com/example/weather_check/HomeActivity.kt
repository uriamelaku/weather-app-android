package com.example.weather_check

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Search button
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        btnSearch.setOnClickListener {
            Toast.makeText(this, "חיפוש עיר", Toast.LENGTH_SHORT).show()
        }

        // History button
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            Toast.makeText(this, "היסטוריה", Toast.LENGTH_SHORT).show()
        }

        // Favorites button
        val btnFavorites = findViewById<Button>(R.id.btnFavorites)
        btnFavorites.setOnClickListener {
            Toast.makeText(this, "מועדפים", Toast.LENGTH_SHORT).show()
        }
    }
}

