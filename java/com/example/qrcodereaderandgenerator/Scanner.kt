package com.example.qrcodereaderandgenerator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.scanner.*

class Scanner : AppCompatActivity() {

    private val MY_CAMERA_REQUEST_CODE = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scanner)

        val qrCodeScanner = qrCodeScanner
    }
}
