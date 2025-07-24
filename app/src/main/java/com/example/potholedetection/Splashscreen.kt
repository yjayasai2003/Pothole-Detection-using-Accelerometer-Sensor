
package com.example.potholedetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView

class Splashscreen : AppCompatActivity() {
    private val SPLASH_SCREEN_TIMEOUT = 3000L
    private lateinit var top: Animation
    private lateinit var bottom: Animation
    private lateinit var slogan: TextView
    private lateinit var image: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        top = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        bottom = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)

        image = findViewById(R.id.image)
        image.startAnimation(top)

        slogan = findViewById(R.id.slogan)
        slogan.startAnimation(bottom)

        Handler().postDelayed({
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_SCREEN_TIMEOUT)
    }
}