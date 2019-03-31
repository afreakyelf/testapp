package com.example.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = LoginFragment()
        val fm  = this@MainActivity.supportFragmentManager
        fm.popBackStack()
        val ft = fm.beginTransaction()
        ft.replace(R.id.main_content,fragment)
        ft.addToBackStack(null)
        ft.commit()

    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1 || supportFragmentManager.backStackEntryCount == 4) {
            this.finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

}
