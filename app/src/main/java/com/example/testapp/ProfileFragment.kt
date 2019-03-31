package com.example.testapp

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_profile.*
import com.bumptech.glide.request.RequestOptions




class ProfileFragment : Fragment() {
        private var mDatabaseReference : DatabaseReference? = null
        private var mPhoneNumber : String?=null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_profile, container, false)
        }


        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            mPhoneNumber = arguments?.getString("mobile")
            mDatabaseReference = FirebaseDatabase.getInstance().reference.child("testapp").child("visitors")

            mDatabaseReference?.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {
                    for (data in p0.children) {
                        if (data.child("phone_number").value == mPhoneNumber) {
                                name.text = data.child("name").value.toString()
                                email.text = data.child("email").value.toString()
                                number.text = data.child("phone_number").value.toString()
                                visits.text = String.format(resources.getString(R.string.visits,data.child("visit_count").value.toString()))

                            val imageUrl = data.child("image").value.toString()
                            if(imageUrl.isEmpty()) {
                                Glide.with(context!!).load(R.drawable.loading).into(clickPicture)
                                Handler().postDelayed({
                                    val imageUrl2 = Uri.parse(data.child("image").value.toString()) as Uri
                                    Glide.with(context!!).load(imageUrl2).into(clickPicture)
                                }, 3000)

                            }else{

                                val requestOptions = RequestOptions().also {
                                    it.placeholder(R.drawable.loading)
                                    it.error(R.drawable.loading)
                                }

                                val imageIro = Uri.parse(imageUrl)
                                Glide.with(context!!).load(imageIro).apply(requestOptions).into(clickPicture)
                            }

                                val snackBar = Snackbar.make(showdetail, String.format(resources.getString(R.string.visits,data.child("visit_count").value.toString())), Snackbar.LENGTH_LONG)
                                snackBar.setAction("Dismiss") { }
                                snackBar.show()

                        }
                    }
                }


                override fun onCancelled(p0: DatabaseError) {
                }


            })

            exit.setOnClickListener {
                activity!!.finish()
            }


        }

}