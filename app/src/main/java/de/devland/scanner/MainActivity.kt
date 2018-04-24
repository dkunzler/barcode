package de.devland.scanner

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphicTracker
import com.google.android.gms.vision.barcode.Barcode
import com.squareup.otto.Subscribe
import de.devland.scanner.event.BarcodeEvent
import de.devland.scanner.event.FragmentSelectionEvent
import de.devland.scanner.event.FragmentType
import kotterknife.bindView


class MainActivity : AppCompatActivity(), BarcodeGraphicTracker.BarcodeUpdateListener {

    private val viewPager: ViewPager by bindView(R.id.viewPager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        App.mainBus.register(this)

        viewPager.adapter = PagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                var fragment = FragmentType.UNKNOWN
                when (position) {
                    0 -> fragment = FragmentType.SCAN
                    1 -> fragment = FragmentType.RESULT
                }

                App.mainBus.post(FragmentSelectionEvent(fragment))
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        App.mainBus.unregister(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ScanFragment.RC_HANDLE_CAMERA_PERM -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // restart activity
                    val intent = intent
                    finish()
                    startActivity(intent)
                } else {
                    finish()
                }
                return
            }
        }
    }

    @Subscribe
    fun onBarcodeEvent(barcodeEvent: BarcodeEvent) {
        // switch to ResultFragment
        viewPager.setCurrentItem(1, true)
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 1) {
            viewPager.setCurrentItem(0, true)
        } else {
            super.onBackPressed()
        }
    }

    class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment? {
            when (position) {
                0 -> return ScanFragment()
                1 -> return ResultFragment()
            }
            return null
        }

        override fun getCount(): Int {
            return 2
        }

    }

    override fun onBarcodeDetected(barcode: Barcode) {
        runOnUiThread({ App.mainBus.post(BarcodeEvent(barcode)) })
    }

}
