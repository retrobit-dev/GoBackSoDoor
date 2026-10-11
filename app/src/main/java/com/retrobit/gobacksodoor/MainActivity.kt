package com.retrobit.gobacksodoor

import android.app.Activity
import android.os.Bundle
import com.retrobit.gobacksodoor.ui.GameView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GameView(this))
    }
}
