package com.example.testapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.camerakit.CameraKitView
import kotlinx.android.synthetic.main.fragment_get_detail.*
import android.util.Log
import android.view.Window
import kotlinx.android.synthetic.main.takepicturedialog.*
import java.io.File
import java.io.FileOutputStream
import android.os.AsyncTask
import android.os.Handler
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar


class GetDetailFragment : Fragment() {

    private var mPhoneNumber : String?=null
    private var mCameraKitView : CameraKitView? = null
    private var dialog: Dialog? = null
    private var path : String?=null
    private val MYPERMISSIONSREQUEST = 11
    private var sEmail : String? = null
    private var sName : String?=null
    private var savedPhoto : File? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_get_detail, container, false)

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPhoneNumber=  arguments?.getString("mobile")


        editPhotoButton.setOnClickListener {


            if (ContextCompat.checkSelfPermission(activity!!,
                    Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                        Manifest.permission.CAMERA)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity!!,
                        arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),MYPERMISSIONSREQUEST)
                }
            } else {
                // Permission has already been granted
                openDialog()
            }

        }

        next.setOnClickListener {

            sEmail = email.text.toString()
            sName  = name.text.toString()

            if(sEmail!!.isEmpty()){
                email.error = "Email Required"
                email.requestFocus()
                return@setOnClickListener
            }

            if(sName!!.isEmpty()){
                name.error = "Name Required"
                name.requestFocus()
                return@setOnClickListener
            }

            if(path.isNullOrEmpty()) {
                editPhotoButton.requestFocus()
                editPhotoButton.error = "Photo required"
                val snackbar = Snackbar.make(parent, "Your Picture is Mandatory.", Snackbar.LENGTH_LONG)
                snackbar.setAction("Dismiss") { }
                snackbar.show()
                return@setOnClickListener
            }

            val fragment = VerifyOtpFragment()
            val fm  = activity?.supportFragmentManager
            val ft = fm?.beginTransaction()
            val bundle = Bundle()
            bundle.putString("mobile",mPhoneNumber)
            bundle.putString("imagePath",path)
            bundle.putString("email",sEmail)
            bundle.putString("name",sName)
            fragment.arguments = bundle
            ft?.replace(R.id.main_content,fragment)
            ft?.addToBackStack(null)
            ft?.commit()

        }

    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            )   {  editPhotoButton.performClick()
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            val snackbar = Snackbar.make(parent, "Permission Denied.", Snackbar.LENGTH_LONG)
            snackbar.setAction("Dismiss") { }
            snackbar.show()
        }
        }
    }


    @SuppressLint("InflateParams")
    private fun openDialog() {
        dialog = Dialog(activity!!)
        val view = LayoutInflater.from(context).inflate(R.layout.takepicturedialog, null)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(view)
        dialog?.setCancelable(true)
        dialog?.camera?.onResume()
        mCameraKitView = dialog?.findViewById(R.id.camera)
        dialog?.takePicture?.setOnClickListener {
        takePicture()
        }

        dialog?.show()

    }

    private fun takePicture() {
        mCameraKitView?.captureImage { _, photo ->
            savedPhoto = File(Environment.getExternalStorageDirectory(), "$mPhoneNumber.jpg")
            try {
                path = savedPhoto?.path
                SavePhotoTask(savedPhoto!!).execute(photo)
                dialog?.dismiss()
                activity!!.runOnUiThread {
                    Handler().postDelayed({
                        val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.fromFile(savedPhoto))
                        if(bitmap!=null) {
                            imageview.setImageBitmap(bitmap)
                        }else{

                            val snackbar = Snackbar.make(parent, "Some error occurred. Please try again.", Snackbar.LENGTH_LONG)
                            snackbar.setAction("Dismiss") { }
                            snackbar.show()


                        }
                    }
                        ,1000)
                }

            } catch (e: java.io.IOException) {
                e.printStackTrace()
                Log.e("CKDemo", "Exception in photo callback")
            }
        }
    }




    class SavePhotoTask(private var savedPhoto: File) : AsyncTask<ByteArray, String, String>() {

        override fun doInBackground(vararg jpeg: ByteArray): String? {

           if (savedPhoto.exists()) {
                savedPhoto.delete()
           }

            try {
              val fos = FileOutputStream(savedPhoto.path)
                fos.write(jpeg[0])
                fos.close()
            } catch (e: java.io.IOException) {
                Log.e("PictureDemo", "Exception in photoCallback", e)
            }

            return null
        }
    }

}
