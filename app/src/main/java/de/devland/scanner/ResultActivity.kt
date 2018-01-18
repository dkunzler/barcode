package de.devland.scanner

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.google.android.gms.vision.barcode.Barcode
import kotterknife.bindView

class ResultActivity : AppCompatActivity() {

    private val resultText: TextView by bindView(R.id.resultText)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val barcode: Barcode? = intent.extras.get(ScanActivity.EXTRA_BARCODE) as Barcode?
        resultText.text = barcode?.rawValue ?: "No result"
    }
}
