package com.example.qrcodereaderandgenerator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color.BLACK
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.util.Base64
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.Writer
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.file.FileOutputStream
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val connectionSTR = "DefaultEndpointsProtocol=https;AccountName=lgsusrmzuwu;AccountKey=Qeouvlk2izMwuCfCX/ru6tMunYdrVyZKSQIw3zGW2y+nr3UNsINx6NhBJqu+meSbe5K9sjEkz+H7S5wHzR4ZJA==;EndpointSuffix=core.windows.net"
    private val requestCameraPicture = 100 //para cuando se tome la foto o en el ActivityResult
    private var byteArrayImg : ByteArray? = null
    private val context = this
    private lateinit var bitmapF : Bitmap
    var currentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGenerar.setOnClickListener {
            checkCameraPermissions()

            var pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(pictureIntent, requestCameraPicture)

        }

        btnLeer.setOnClickListener {
            try{
                var intent = Intent("com.google.zxing.client.android.SCAN")
                intent.putExtra("SCAN_MODE","QR_CODE_MODE")
                startActivityForResult(intent,0)
            }catch (e : Exception)
            {
                val marketUri = Uri.parse("market://details?id=com.google.zxing.client.android")
                val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
                startActivity(marketIntent)
            }
        }

    }

    private class InsertBLobImg(val connectionSTR : String, val byteArray : ByteArray?): AsyncTask<Void,Void,Bitmap?>()
    {
        var imgBit : Bitmap? = null

        override fun doInBackground(vararg params: Void?): Bitmap? {

            try {
                //Sorage Acc from connection String
                val storageAcc: CloudStorageAccount = CloudStorageAccount.parse(connectionSTR)     //AQUI CRASHEA
                //Create blob client
                val blobClient: CloudBlobClient = storageAcc.createCloudBlobClient()
                //Get reference to a container
                val container: CloudBlobContainer = blobClient.getContainerReference("imagenes")
                var resourceBlob = container.getBlockBlobReference("ImagenFinal" + ".jpg") //donde se pasa la imag

                //resourceBlob.uploadFromByteArray(byteArray, 4, byteArray!!.size)
                resourceBlob.upload(byteArray!!.inputStream(),byteArray!!.size.toLong())

                var rutaBlob = resourceBlob.storageUri.primaryUri.toString()

                var multiFormatW = MultiFormatWriter()
                try {
                    var bitMatrix: BitMatrix = multiFormatW.encode(rutaBlob, BarcodeFormat.QR_CODE, 200, 200)
                    var barcodeEncoder = BarcodeEncoder()
                    var bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
                    imgBit = bitmap
                    return imgBit
                } catch (e: WriterException) {
                    e.printStackTrace()
                } catch (e: StorageException) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: StorageException) {
                e.printStackTrace()
            }

            return imgBit
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //TAMBIEN TENDREMOS QUE PONER UN WHEN PARA QUE VEA EL CODIGO DE ACEPTACION
        when(requestCode)
        {
            requestCameraPicture -> {
                //Comprobar si el resultado es bueon
                if (resultCode == Activity.RESULT_OK)
                {
                    //Obtenemos los extras del intent recibido por parametros
                    val extras = data!!.extras
                    //Formamos el bitMap a partir d elso extras
                    val imageBitMap = extras.get("data") as Bitmap






                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imageBitMap.compress(Bitmap.CompressFormat.JPEG,90, byteArrayOutputStream)
                    var byteArray : ByteArray = byteArrayOutputStream.toByteArray()

                    byteArrayImg = byteArray

                    bitmapF = InsertBLobImg(connectionSTR,byteArrayImg).execute().get()!!
                    if (bitmapF != null){
                        imgView.setImageBitmap(bitmapF)
                    }


                }else
                {
                    //LA FOTO NO HA SIDO TOMADA CON EXITO
                    Toast.makeText(this, "Picture has failed", Toast.LENGTH_SHORT).show()
                }
            }

            0 -> {
                if (resultCode == Activity.RESULT_OK)
                {
                    var contents : String = data!!.getStringExtra("SCAN_RESULT")
                    Toast.makeText(this,contents, Toast.LENGTH_SHORT).show()
                    //Picasso.with(context).load(contents).fit().into(imgView)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(contents))
                    startActivity(browserIntent)
                }
                else
                {
                    if (requestCode == Activity.RESULT_CANCELED)
                    {
                        Toast.makeText(this,"ERROR EN EL SCANEO", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkCameraPermissions() = setCameraPermissionHandlerWithSnackBar()


    private fun setCameraPermissionHandlerWithSnackBar(){

        val snackBarPErmissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(root, "Camera is needed to take pictures")
            .withOpenSettingsButton("Settings") //PARA ABRIR LOS AJUSTES DE LA APLICACION EN ANDROID
            .withCallback(object : Snackbar.Callback(){
                override fun onShown(sb: Snackbar?) {
                    //EVENT HANDLER FOR WHEN THE GIVEN SNACKBAR IS VISIBLE
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    //EVENT HANDLER FOR WHEN THE GIVEN SNACKBAR IS DISSMISED
                }
            }).build()

        val permission = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                Toast.makeText(context,"Granted",Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                if (response!!.isPermanentlyDenied)
                {
                    Toast.makeText(context,"Permanently denied",Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context,"Denied",Toast.LENGTH_SHORT).show()

                }
            }

            override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                token?.continuePermissionRequest()
            }

        }

        //PARRA USAR EL COMPOSITE
        val composite = CompositePermissionListener(permission, snackBarPErmissionListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(composite)
            .check()
    }
}
