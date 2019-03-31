package com.example.testapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.fragment_verify_otp.*
import java.util.concurrent.TimeUnit
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.net.Uri
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.UploadTask
import java.io.File
import java.lang.Exception


class VerifyOtpFragment : Fragment() {

    private var mAuth : FirebaseAuth?= null
    private var mMobileNumber : String? = null
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var mDatabaseReference : DatabaseReference? = null
    private var mImagePath : String ? =null
    private var sEmail : String? = null
    private var sName : String?=null
    private var isLoggedIn : Boolean? = false
    private var mStorageReference : StorageReference? = null
    private var mCountDownTimer : CountDownTimer? = null



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_verify_otp, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FirebaseApp.initializeApp(context!!)
        mAuth = FirebaseAuth.getInstance()

        mMobileNumber = arguments?.getString("mobile")
        mImagePath = arguments?.getString("imagePath")
        sEmail = arguments?.getString("email")
        sName = arguments?.getString("name")
        sendVerificationCode(mMobileNumber)

        progressBar_cyclic.visibility  = View.GONE
        verifyOtp.visibility = View.VISIBLE

        val spannableString = SpannableString(resources.getString(R.string.numberText)+mMobileNumber)
        val foregroundSpan =  ForegroundColorSpan(Color.BLACK)
        spannableString.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), spannableString.length-13,spannableString.length , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(foregroundSpan, spannableString.length-13,spannableString.length , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        mobiletext.text = spannableString

        verifyOtp.setOnClickListener {
            progressBar_cyclic.visibility  = View.VISIBLE
            verifyOtp.visibility = View.GONE
            val code = firstPinView.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                firstPinView.error = "Enter valid code"
                firstPinView.requestFocus()
                progressBar_cyclic.visibility  = View.GONE
                verifyOtp.visibility = View.VISIBLE
                return@setOnClickListener
            }
            //verifying the code entered manually
            verifyVerificationCode(code)
        }


        mCountDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onFinish() {
                if(isLoggedIn==false) {
                    invalidOtp()
                }
            }
            override fun onTick(millisUntilFinished: Long) {
                try {
                    timer.text = (millisUntilFinished / 1000).toString()
                }catch (e:Exception){}
            }
        }.start()



    }

    private fun sendVerificationCode(mMobileNumber: String?) {


        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential?) {
                //Getting the code sent by SMS
                val code = phoneAuthCredential?.smsCode
                if(code!=null){
                    firstPinView.setText(code)
                    verifyVerificationCode(code)
                }
            }
            override fun onVerificationFailed(p0: FirebaseException?) {
                Toast.makeText(context, p0?.message, Toast.LENGTH_LONG).show()
            }
            override fun onCodeSent(
                verificationId: String?,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                    Log.d("codeSent", "onCodeSent:" + verificationId!!)
                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token
            }
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            "+91$mMobileNumber",   // Phone number to verify
            30,                    // Timeout duration
            TimeUnit.SECONDS,      // Unit of timeout
            activity!!,                  // Activity (for callback binding)
            callbacks
        )
    }

    private fun verifyVerificationCode(code: String) {
        try {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId!!,code)
            signInWithPhoneAuthCredential(credential)
        }catch (e : Exception){
            Log.d("exception",e.toString())
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential?) {

        progressBar_cyclic.visibility  = View.VISIBLE
        verifyOtp.visibility = View.GONE

        mAuth?.signInWithCredential(credential!!)!!
            .addOnCompleteListener(activity!!){task ->
                mDatabaseReference = FirebaseDatabase.getInstance().reference.child("testapp")
                mStorageReference = FirebaseStorage.getInstance().reference
                if (task.isSuccessful){
                    mCountDownTimer?.cancel()
                    val visitCount = 1
                    val mTempDatabaseReference = mDatabaseReference?.child("visitors")?.push()
                    mTempDatabaseReference?.child("phone_number")?.setValue(mMobileNumber!!)
                    mTempDatabaseReference?.child("visit_count")?.setValue(visitCount)
                    mTempDatabaseReference?.child("name")?.setValue(sName!!)
                    mTempDatabaseReference?.child("email")?.setValue(sEmail)
                    val ref = mStorageReference?.child("images/$mMobileNumber.jpg")
                    val  uploadTask = ref?.putFile(Uri.fromFile(File(mImagePath)))
                    uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>>
                    { task2 ->
                        if (!task2.isSuccessful) {
                            task2.exception?.let {
                                throw it
                            }
                        }
                        return@Continuation ref.downloadUrl
                    })?.addOnCompleteListener { task2 ->
                        if (task2.isSuccessful) {
                            val downloadUri = task2.result
                            mTempDatabaseReference?.child("image")?.setValue(downloadUri.toString())
                            progressBar_cyclic.visibility  = View.GONE
                            verifyOtp.visibility = View.VISIBLE
                            val fragment = ProfileFragment()
                            val fm  = activity?.supportFragmentManager
                            val ft = fm?.beginTransaction()
                            val bundle = Bundle()
                            bundle.putString("mobile",mMobileNumber)
                            fragment.arguments = bundle
                            ft?.replace(R.id.main_content,fragment)
                            ft?.commit()
                        } else {
                            // Handle failures
                            // ...
                        }
                    }
                    isLoggedIn = true
                }


                else{
                    mCountDownTimer?.cancel()
                    var message = "Somthing is wrong, we will fix it soon..."
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        message = "Invalid OTP entered..."
                        invalidOtp()
                        isLoggedIn = false
                    }
                    val snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
                    snackbar.setAction("Dismiss") { }
                    snackbar.show()
                    progressBar_cyclic.visibility  = View.GONE
                    verifyOtp.visibility = View.VISIBLE
                }
            }
    }


    private fun invalidOtp(){

        mDatabaseReference = FirebaseDatabase.getInstance().reference.child("testapp")
        mStorageReference = FirebaseStorage.getInstance().reference
        val mTempDatabaseReference = mDatabaseReference?.child("suspicious_users")?.push()
        mTempDatabaseReference?.child("phone_number")?.setValue(mMobileNumber!!)
        mTempDatabaseReference?.child("name")?.setValue(sName!!)
        mTempDatabaseReference?.child("email")?.setValue(sEmail)
        val ref = mStorageReference?.child("images/suspicious/$mMobileNumber.jpg")
        val  uploadTask = ref?.putFile(Uri.fromFile(File(mImagePath)))
        uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>>
        { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        })?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                mTempDatabaseReference?.child("image")?.setValue(downloadUri.toString())
            } else {
                // Handle failures
                // ...
            }
        }

        val fragment = LoginFragment()
        val fm  = activity?.supportFragmentManager
        val ft = fm?.beginTransaction()
        ft?.replace(R.id.main_content,fragment)
        ft?.commit()
    }

}







