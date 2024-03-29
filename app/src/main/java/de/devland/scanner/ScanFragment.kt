package de.devland.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic
import com.google.android.gms.samples.vision.barcodereader.BarcodeTrackerFactory
import com.google.android.gms.samples.vision.barcodereader.ui.camera.GraphicOverlay
import com.squareup.otto.Subscribe
import de.devland.scanner.event.FragmentSelectionEvent
import de.devland.scanner.event.FragmentType
import kotlinx.android.synthetic.main.fragment_scan.*
import kotterknife.bindView
import me.dm7.barcodescanner.zbar.ZBarScannerView
import java.io.IOException


class ScanFragment : Fragment() {

    companion object {
        const val TAG = "ScanFragment"
        const val RC_HANDLE_CAMERA_PERM = 2
        const val RC_HANDLE_GMS = 9001
    }

    private val scanPreview: ZBarScannerView by bindView(R.id.preview)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.mainBus.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rc = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(activity, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(activity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        topLayout.setOnClickListener(listener)
        Snackbar.make(scanPreview, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, listener)
                .show()
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource() {
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @SuppressLint("MissingPermission")
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                activity.applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(activity, code, RC_HANDLE_GMS)
            dlg.show()
        }

        if (cameraSource != null) {
            try {
                scanPreview.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        scanPreview.stop()
    }

    @Subscribe
    fun onFragmentSelection(event: FragmentSelectionEvent) {
        Thread({
            if (event.fragment == FragmentType.SCAN) {
                startCameraSource()
            } else {
                scanPreview.stop()
            }
        }).start()
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        scanPreview.release()
        App.mainBus.unregister(this)
    }
}
