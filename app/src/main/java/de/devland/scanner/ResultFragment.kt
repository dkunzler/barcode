package de.devland.scanner

import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.vision.barcode.Barcode
import com.squareup.otto.Subscribe
import de.devland.scanner.event.BarcodeEvent
import kotterknife.bindView
import org.json.JSONException
import org.json.JSONObject

class ResultFragment : Fragment() {

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val resultText: TextView by bindView(R.id.resultText)

    private var currentBarcode: Barcode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.mainBus.register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        App.mainBus.unregister(this)
    }

    @Subscribe
    fun onBarcodeEvent(barcodeEvent: BarcodeEvent) {
        currentBarcode = barcodeEvent.barcode
        // try to parse a json object
        processBarcode(barcodeEvent.barcode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (currentBarcode != null) {
            outState.putParcelable("result", currentBarcode)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.app_name)
        resultText.typeface = Typeface.createFromAsset(context.assets, "fonts/RobotoSlab-Light.ttf")
        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            val barcode: Barcode = savedInstanceState.getParcelable("result")
            currentBarcode = barcode
            processBarcode(barcode)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_result, container, false)
    }


    private fun ResultFragment.processBarcode(barcode: Barcode) {
        try {
            val json = JSONObject(barcode.rawValue)
            resultText.text = json.toString(2)
        } catch (e: JSONException) {
            // parsing failed, use raw text and Linkify for result
            resultText.text = barcode.rawValue
            Linkify.addLinks(resultText, Linkify.ALL)
        }
    }
}
