package com.example.greenbook.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import com.example.greenbook.Database
import com.example.greenbook.R
import com.example.greenbook.dataObjekter.Profil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*


class PrivatProfilFragment : Fragment() {
    private lateinit var button_privat_folgere:Button
    private lateinit var button_privat_RedigerBilde:Button
    private lateinit var button_privat_RedigerBio:Button
    private lateinit var imageView_privat_profilBilde: ImageView
    private lateinit var textView_privat_navn: TextView
    private lateinit var textView_privat_bioTekst: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var database: Database
    private lateinit var uri:Uri
    private lateinit var dinProfil:Profil
    private val storage = FirebaseStorage.getInstance()
    private val pickerContent = registerForActivityResult(ActivityResultContracts.GetContent()){
        uri = it
        Picasso.get().load(uri).into(imageView_privat_profilBilde)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        user = auth.currentUser!!
        database = Database()


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_privat_folgere = requireView().findViewById(R.id.profil_privat_følgere)
        button_privat_RedigerBilde = requireView().findViewById(R.id.profil_privat_button_redigerBilde)
        imageView_privat_profilBilde = requireView().findViewById(R.id.profil_privat_profilBilde)
        textView_privat_bioTekst = requireView().findViewById(R.id.profil_privat_txt_bio)
        textView_privat_navn = requireView().findViewById(R.id.profil_privat_navn)
        button_privat_RedigerBio = requireView().findViewById(R.id.profil_privat_btn_redigerBio)

       updateUI()

       button_privat_RedigerBilde.setOnClickListener {
            pickerContent.launch("image/*")
       }

       button_privat_RedigerBio.setOnClickListener {
            updateProfil()
       }

    }

    private fun updateProfil() {
        var bildeURL: String? = null
        val path = "Profil/" + UUID.randomUUID() + ".png"
        val profilRef = storage.getReference(path)
        val uploadTask = profilRef.putFile(uri)
        val getDownloadUriTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            profilRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                bildeURL = task.result.toString()
                val profil = Profil(
                    dinProfil.email,
                    dinProfil.fornavn,
                    dinProfil.etternavn,
                    dinProfil.fdato,
                    bildeURL,
                    textView_privat_bioTekst.text.toString()
                )
                database.addBruker(auth.uid.toString(),profil)
            }
        }
        Toast.makeText(context, "Profil er oppdatert", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    private fun updateUI(){
        val profilListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profil = snapshot.getValue<Profil>()!!
                dinProfil = profil
                update(profil!!)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        database.database.child("bruker").child(auth.uid.toString()).addValueEventListener(profilListener)

        val følgereListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val joined = snapshot.childrenCount.toInt()
                if(joined == 1)
                    button_privat_folgere.text = joined.toString() + "følgere"
                else
                    button_privat_folgere.text = joined.toString() + " følger"
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        database.database.child("followers").child(auth.uid.toString()).addValueEventListener(følgereListener)
    }

    @SuppressLint("SetTextI18n")
    private fun update(profil: Profil){
        textView_privat_navn.text = profil.fornavn + " " + profil.etternavn
        textView_privat_bioTekst.setText(profil.bio)
        if(profil.imgUrl !=null)
            Picasso.get().load(profil.imgUrl).into(imageView_privat_profilBilde)
    }

    companion object {
        fun newInstance() = PrivatProfilFragment()
    }
}