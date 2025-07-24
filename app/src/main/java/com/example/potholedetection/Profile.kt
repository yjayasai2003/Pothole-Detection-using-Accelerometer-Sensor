package com.example.potholedetection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class Profile : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logout()
        }
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        // Displaying username
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        //val useremail = arguments?.getString("userEmail")
        usernameTextView.text = "Username: $userEmail"
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), SignIn::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}
