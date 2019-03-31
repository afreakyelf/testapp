package com.example.testapp

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.loginfragment.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener


class LoginFragment : Fragment() {
    private var mPhoneNumber : String?=null
    private var mDatabaseReference : DatabaseReference? = null
    private var mUserId : String? = null
    private var mUserExist : Boolean? = true
    private var mAuth : FirebaseAuth?=null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.loginfragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        FirebaseApp.initializeApp(context!!)
        mAuth = FirebaseAuth.getInstance()


        progressBar_cyclic.visibility  = View.GONE
        login.visibility = View.VISIBLE

        login.setOnClickListener {

            progressBar_cyclic.visibility  = View.VISIBLE
            login.visibility = View.GONE

            mPhoneNumber = number.text.toString()



            if(mPhoneNumber!!.isEmpty() || mPhoneNumber!!.length < 10){
                number.error = getString(R.string.enter_a_valid_mobile)
                number.requestFocus()
                progressBar_cyclic.visibility  = View.GONE
                login.visibility = View.VISIBLE
                return@setOnClickListener
            }


            mDatabaseReference = FirebaseDatabase.getInstance().reference.child("testapp").child("visitors")
            mUserId = mAuth?.currentUser?.uid



            mDatabaseReference?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.value!=null){
                    for (data in dataSnapshot.children) {
                        if (data.child("phone_number").value==mPhoneNumber) {
                            val lastVisitCount = data.child("visit_count").value.toString()
                            val updatedVisitCount = lastVisitCount.toInt()+1
                            mDatabaseReference?.child(data.key!!)?.child("visit_count")?.setValue(updatedVisitCount.toString())
                            val snackBar = Snackbar.make(parentMain,
                                "welcome back for $updatedVisitCount time", Snackbar.LENGTH_LONG)
                            snackBar.setAction("Dismiss") { }
                            snackBar.show()
                            mUserExist = true

                            val fragment = ProfileFragment()
                            val fm  = activity?.supportFragmentManager
                            val ft = fm?.beginTransaction()
                            val bundle = Bundle()
                            bundle.putString("mobile",mPhoneNumber)
                            fragment.arguments = bundle
                            ft?.replace(R.id.main_content,fragment)
                            ft?.commit()

                            progressBar_cyclic.visibility  = View.GONE
                            login.visibility = View.VISIBLE


                            break

                        } else {
                            //if user does not exists
                            mUserExist = false
                            progressBar_cyclic.visibility  = View.GONE
                            login.visibility = View.VISIBLE

                        }
                    }}else{
                        // If dataSnapshot is null
                        progressBar_cyclic.visibility  = View.GONE
                        login.visibility = View.VISIBLE
                        mUserExist = false
                    }

                    if(mUserExist==false){
                        val fragment = GetDetailFragment()
                        val fm  = activity?.supportFragmentManager
                        val ft = fm?.beginTransaction()
                        val bundle = Bundle()
                        bundle.putString("mobile",mPhoneNumber)
                        fragment.arguments = bundle
                        ft?.replace(R.id.main_content,fragment)
                        ft?.addToBackStack(null)
                        ft?.commit()
                    }

                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })


        }


    }
}