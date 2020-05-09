package com.pranjal.video.activities
import android.content.Intent
import android.graphics.ColorSpace
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.VideoView

import com.pranjal.video.R


class Splash: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            val startAct = Intent(this@Splash, LoginActivity::class.java)
            startActivity(startAct)
        }, 3000)

    }
    override fun onStop() {
        super.onStop()
        finish()
    }
}
