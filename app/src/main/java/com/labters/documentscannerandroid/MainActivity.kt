/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:17 PM
 *
 */

package com.labters.documentscannerandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kotlinpermissions.KotlinPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var btnPick: Button
    lateinit var imgBitmap: ImageView
    lateinit var photoPath: String

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1111 && resultCode == RESULT_OK && data != null) {
            startActivity(ImageCropActivity.newIntent(this, data.data.toString()))
        } else if (requestCode == 1231 && resultCode == Activity.RESULT_OK) {
            startActivity(ImageCropActivity.newIntent(this, photoPath))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnPick = findViewById(R.id.btnPick)
        imgBitmap = findViewById(R.id.imgBitmap)
        askPermission()
    }

    fun askPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            KotlinPermissions.with(this)
                .permissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .onAccepted { permissions ->
                    setView()
                }
                .onDenied { permissions ->
                    askPermission()
                }
                .onForeverDenied { permissions ->
                    Toast.makeText(
                        MainActivity@ this,
                        "You have to grant the permissions! Grant them from app settings please.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .ask()
        } else {
            setView()
        }
    }

    fun setView() {
        btnPick.setOnClickListener(View.OnClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("MaliY")
            builder.setMessage("Where would you like to choose the image?")
            builder.setPositiveButton("Gallery") { dialog, which ->
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, 1111)
            }
            builder.setNegativeButton("Camera") { dialog, which ->
                dialog.dismiss()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (ex: IOException) {
                        Log.i("Main", "IOException")
                    }
                    if (photoFile != null) {
                        val builder = StrictMode.VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                        startActivityForResult(cameraIntent, 1231)
                    }
                }
            }
            builder.setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        })
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val image = File.createTempFile(
            imageFileName, // prefix
            ".jpg", // suffix
            storageDir      // directory
        )

        // Save a file: path for use with ACTION_VIEW intents
        photoPath = "file:" + image.absolutePath
        return image
    }

}
