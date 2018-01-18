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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.samples.vision.barcodereader.BarcodeGraphic
import com.google.android.gms.samples.vision.barcodereader.BarcodeTrackerFactory
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSourcePreview
import com.google.android.gms.samples.vision.barcodereader.ui.camera.GraphicOverlay
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.squareup.otto.Subscribe
import de.devland.scanner.event.FragmentSelectionEvent
import de.devland.scanner.event.FragmentType
import kotterknife.bindView
import java.io.IOException


class ScanFragment : Fragment() {

    companion object {
        const val TAG = "ScanFragment"
        const val RC_HANDLE_CAMERA_PERM = 2
        const val RC_HANDLE_GMS = 9001
    }

    private var cameraSource: CameraSource? = null
    private val scanPreview: CameraSourcePreview by bindView(R.id.preview)
    private val graphicOverlay: GraphicOverlay<BarcodeGraphic> by bindView(R.id.graphicOverlay)
    private val topLayout: View by bindView(R.id.topLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.mainBus.register(this)
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()
        App.mainBus.unregister(this)
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

        Snackbar.make(graphicOverlay, "Tap found QR Code to see content",
                Snackbar.LENGTH_LONG)
                .show()
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
        Snackbar.make(graphicOverlay, R.string.permission_camera_rationale,
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
        val context = activity.applicationContext

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(graphicOverlay, activity)
        barcodeDetector.setProcessor(
                MultiProcessor.Builder<Barcode>(barcodeFactory).build())

        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.")

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            @Suppress("DEPRECATION")
            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = activity.registerReceiver(null, lowStorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(activity, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w(TAG, getString(R.string.low_storage_error))
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        var builder: CameraSource.Builder = CameraSource.Builder(context, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)

        // make sure that auto focus is an available option
        builder = builder.setAutoFocusEnabled(true)

        cameraSource = builder.build()
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
        if (event.fragment == FragmentType.SCAN) {
            startCameraSource()
        } else {
            scanPreview.stop()
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        scanPreview.release()
    }
}
