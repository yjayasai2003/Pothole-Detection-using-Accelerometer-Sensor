package com.example.potholedetection

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.potholedetection.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignIn : AppCompatActivity()
    {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()
        binding.register.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        binding.login.setOnClickListener {


            val progressDialog = ProgressDialog(this@SignIn).apply {
                setTitle("Logging in")
                setMessage("Please wait...")
                setCancelable(false)
                show()
            }

            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful)
                    {
                        progressDialog.dismiss()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("userEmail", email)
                        editor.apply()
                        val userEmail = FirebaseAuth.getInstance().currentUser?.email

                        val bundle = Bundle().apply {
                            putString("userEmail", userEmail)
                        }
                        val profileFragment = Profile().apply {
                            arguments = bundle
                        }

                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
    }

    override fun onStart() {
        super.onStart()

        if(firebaseAuth.currentUser != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}