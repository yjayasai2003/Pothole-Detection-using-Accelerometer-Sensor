package com.example.potholedetection

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.potholedetection.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        }

        binding.register.setOnClickListener {
            val progressDialog = ProgressDialog(this).apply {
                setTitle("Registering")
                setMessage("Please wait...")
                setCancelable(false)
                show()
            }

            val email = binding.emailEt.text.toString()
            val password = binding.passET.text.toString()
            val confirmPassword = binding.confirmPassEt.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            progressDialog.dismiss()
                            if (task.isSuccessful) {
                                // Sign up successful, navigate to MainActivity
                                Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish() // Prevent going back to SignUp activity
                            } else {
                                // Sign up failed, display error message
                                Toast.makeText(
                                    this, "Sign Up Failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("SignUp", "Sign Up Failed", task.exception)
                            }
                        }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
