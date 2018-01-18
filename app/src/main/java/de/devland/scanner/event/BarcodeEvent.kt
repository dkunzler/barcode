package de.devland.scanner.event

import com.google.android.gms.vision.barcode.Barcode

/**
* @author David Kunzler (dk@devland.de)
*/
data class BarcodeEvent(val barcode: Barcode)