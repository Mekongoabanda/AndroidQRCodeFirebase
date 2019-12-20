package esirem.com.androidqrcodefirebase

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.wonderkiln.camerakit.*
import dmax.dialog.SpotsDialog

class MainActivity : AppCompatActivity() {
    var cameraView: CameraView? = null
    var btnDetect: Button? = null
    var waitingDialog: AlertDialog? = null

    override fun onResume() {
        super.onResume()
        //Lorsque l'activité reprend la camera redemarre
        cameraView!!.start()
    }

    override fun onPause() {
        super.onPause()
        //Lorsque l'activité reprend la camera s'arrête
        cameraView!!.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        //Appareil photo en mode portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        cameraView = findViewById<View>(R.id.cameraView) as CameraView
        btnDetect = findViewById<View>(R.id.btn_detect) as Button

        //Dialogue d'alerte
        waitingDialog = SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Patientez s'il-vous-plait")
                .setCancelable(false)
                .build()

        //Lorsque l'on click sur le bouton
        btnDetect!!.setOnClickListener {
            //La camera se met en marche
            cameraView!!.start()
            //La camera capture l'image
            cameraView!!.captureImage()
        }

        //Evenement lié à la lecture de la camera
        cameraView!!.addCameraKitListener(object : CameraKitEventListener {

            override fun onEvent(cameraKitEvent: CameraKitEvent) {}
            override fun onError(cameraKitError: CameraKitError) {}

            //Dans le cas d'une Image
            override fun onImage(cameraKitImage: CameraKitImage) {
                waitingDialog!!.show()
                var bitmap = cameraKitImage.bitmap
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView!!.width, cameraView!!.height, false)
                cameraView!!.stop()
                runDetector(bitmap)
            }

            override fun onVideo(cameraKitVideo: CameraKitVideo) {}
        })
    }

    private fun runDetector(bitmap: Bitmap) {

        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_QR_CODE,
                        FirebaseVisionBarcode.FORMAT_PDF417 // Format de l'image (extension)
                ).build()
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        detector.detectInImage(image)
                .addOnSuccessListener { firebaseVisionBarcodes ->
                    //Si la detection de l'image est un succès on appel la procéure suivante
                    processResult(firebaseVisionBarcodes)
                }
                .addOnFailureListener { e -> Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show() }
    }

    //Procedure qui contient l'algorithme que le'on effectuera en cas de réussite de la detection du QR code
    private fun processResult(firebaseVisionBarcodes: List<FirebaseVisionBarcode>) {

        for (item in firebaseVisionBarcodes) {

            val value_type = item.valueType
            when (value_type) {
                FirebaseVisionBarcode.TYPE_TEXT -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(item.rawValue)
                    builder.setPositiveButton("OK") { dialogInterface, which -> dialogInterface.dismiss() }
                    val dialog = builder.create()
                    dialog.show()
                }

                FirebaseVisionBarcode.TYPE_URL -> {
                    //Commencer la recherche de l'url
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.rawValue))
                    startActivity(intent)
                }
                
                FirebaseVisionBarcode.TYPE_CONTACT_INFO -> {
                    val info = StringBuilder("Name : ")
                            .append(item.contactInfo!!.name!!.formattedName)
                            .append("\n")
                            .append("Address : ")
                            .append(item.contactInfo!!.addresses[0].addressLines)
                            .append("\n")
                            .append("Email : ")
                            .append(item.contactInfo!!.emails[0].address)
                            .toString()
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(info)
                    builder.setPositiveButton("OK") { dialogInterface, which -> dialogInterface.dismiss() }
                    val dialog = builder.create()
                    dialog.show()
                }
                else -> {
                }
            }
        }
        waitingDialog!!.dismiss()
    }
}