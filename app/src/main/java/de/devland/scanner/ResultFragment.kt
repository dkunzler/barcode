package de.devland.scanner

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.otto.Subscribe
import de.devland.scanner.event.BarcodeEvent
import kotterknife.bindView
import org.json.JSONException
import org.json.JSONObject

class ResultFragment : Fragment() {

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val resultText: TextView by bindView(R.id.resultText)

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
        // try to parse a json object
        try {
            val json = JSONObject(barcodeEvent.barcode.rawValue)
            resultText.text = json.toString(2)
        } catch (e: JSONException) {
            // parsing failed, use raw text and Linkify for result
            resultText.text = barcodeEvent.barcode.rawValue
            Linkify.addLinks(resultText, Linkify.ALL)
        }


    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.app_name)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_result, container, false)
    }
}
