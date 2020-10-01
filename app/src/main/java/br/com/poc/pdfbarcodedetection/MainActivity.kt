package br.com.poc.pdfbarcodedetection

import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val barcodeScanner = BarcodeScanning.getClient()
    private val image: ImageView by lazy { findViewById(R.id.image) }
    private val pickFile: View by lazy { findViewById(R.id.pick_file) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pickFile.setOnClickListener {
            pickFile()
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/pdf")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_CODE_PDF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val selectedFile = data?.data
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PDF && selectedFile != null) {
            val fileDescriptor = contentResolver.openFileDescriptor(selectedFile, "r")
            if (fileDescriptor != null) {
                readBarcode(fileDescriptor)
            }
        }
    }

    private fun readBarcode(selectedFile: ParcelFileDescriptor) {
        val renderer = PdfRenderer(selectedFile)
        val pageCount = renderer.pageCount
        val paint = Paint().apply {
            color = Color.WHITE
        }

        for (i in 0 until pageCount) {
            val page: PdfRenderer.Page = renderer.openPage(i)

            val maxSize = max(page.width, page.height)
            val scale = TARGET_SIZE / maxSize
            val (width, height) = scale * page.width to scale * page.height

            val pageBitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)

            // Draw white background
            Canvas(pageBitmap).drawRect(Rect(0, 0, pageBitmap.width, pageBitmap.height), paint)

            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            if (i == 0) {
                image.setImageBitmap(pageBitmap)
            }

            recognizeBarcode(pageBitmap)
        }

        renderer.close()
    }

    private fun recognizeBarcode(pageBitmap: Bitmap) {
        barcodeScanner.process(InputImage.fromBitmap(pageBitmap, 0))
            .addOnSuccessListener {
                if (it.isNotEmpty()) {
                    val barcodes = it[0].displayValue!!
                    val type = it[0].valueType

                    val message = """
                                    Barcode: $barcodes
                                    Type: $type""".trimIndent()

                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                }
            }
    }

    companion object {
        private const val REQUEST_CODE_PDF = 1
        private const val TARGET_SIZE = 2600f
    }
}