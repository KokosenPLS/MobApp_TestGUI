package com.example.greenbook.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.core.view.isInvisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.greenbook.Database
import com.example.greenbook.NavGraphDirections
import com.example.greenbook.R
import com.example.greenbook.adaptorClasses.PostAdaptor
import com.example.greenbook.dataObjekter.Arrangement
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
import kotlin.collections.ArrayList

class PrivatProfilFragment : Fragment(R.layout.fragment_profil), PostAdaptor.OnItemClickListener{

    private val args: PrivatProfilFragmentArgs by navArgs()

    private lateinit var button_privat_folgere:Button
    private lateinit var button_privat_RedigerBilde:Button
    private lateinit var button_privat_RedigerBio:Button
    private lateinit var imageView_privat_profilBilde: ImageView
    private lateinit var textView_privat_navn: TextView
    private lateinit var textView_privat_bioTekst: EditText
    private lateinit var recyclerView: RecyclerView
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

    private lateinit var påmeldteArrangement: ArrayList<Arrangement>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = Uri.EMPTY // må sette den til tom i starten, for å sjekke om den er tom senere
        auth = Firebase.auth
        user = auth.currentUser!!
        database = Database()
        påmeldteArrangement = ArrayList()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_privat_folgere = requireView().findViewById(R.id.profil_privat_følgere)
        button_privat_RedigerBilde = requireView().findViewById(R.id.profil_privat_button_redigerBilde)
        imageView_privat_profilBilde = requireView().findViewById(R.id.profil_privat_profilBilde)
        textView_privat_bioTekst = requireView().findViewById(R.id.profil_privat_txt_bio)
        textView_privat_navn = requireView().findViewById(R.id.profil_privat_navn)
        button_privat_RedigerBio = requireView().findViewById(R.id.profil_privat_btn_redigerBio)

        recyclerView = view.findViewById(R.id.profil_privat_recyclerview)
        val adapter = PostAdaptor(påmeldteArrangement, this, requireContext())
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter
        hentArrangement(adapter)

        textView_privat_bioTekst.setInputType(0)
        button_privat_RedigerBilde.visibility= Button.INVISIBLE
        updateUI()

       button_privat_RedigerBilde.setOnClickListener {
           if (button_privat_RedigerBio.text.equals("Update")) {
               Toast.makeText(context, "Du må trykke på update, for å endre bilde", Toast.LENGTH_SHORT).show()
           } else {
               pickerContent.launch("image/*")
           }
       }

       button_privat_RedigerBio.setOnClickListener {
           if(button_privat_RedigerBio.text.equals("Update")) {
               button_privat_RedigerBio.text = "LAGRE"
               textView_privat_bioTekst.setInputType(1)
               button_privat_RedigerBilde.visibility= Button.VISIBLE
           }
           else {
               updateProfil()
               textView_privat_bioTekst.setInputType(0)
               textView_privat_bioTekst.setKeyListener(null)
               button_privat_RedigerBio.text = "Update"
               button_privat_RedigerBilde.visibility= Button.INVISIBLE
           }
       }

       button_privat_folgere.setOnClickListener {
           val action =  PrivatProfilFragmentDirections.actionProfilFragmentToFoolgerFragment(auth.uid.toString(), textView_privat_navn.text.toString())
           findNavController().navigate(action)
       }

    }

    private fun hentArrangement(adaptor: PostAdaptor) {

        val arrangementListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val arrangement = snapshot.getValue<Arrangement>()
                    var found = false
                    for(arr in påmeldteArrangement){
                        if (arr.equals(arrangement))
                            found = true
                    }
                    if(!found)
                        påmeldteArrangement.add(arrangement!!)
                }
                adaptor.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        val påmeldingerListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    for(påmelding in snapshot.children){
                        database.database.child("arrangement").child(påmelding.key!!).addValueEventListener(arrangementListener)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        database.database.child("påmeldt").child(args.brukerID).addValueEventListener(påmeldingerListener)

    }

    private fun updateProfil() {
        if(uri == Uri.EMPTY) {
            val profil = Profil(
                dinProfil.brukerId,
                dinProfil.email,
                dinProfil.fornavn,
                dinProfil.etternavn,
                dinProfil.fdato,
                dinProfil.imgUrl,
                textView_privat_bioTekst.text.toString()
            )
            database.addBruker(auth.uid.toString(), profil)
            Toast.makeText(context, "Profil er oppdatert", Toast.LENGTH_SHORT).show()
        }
        else {
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
                        dinProfil.brukerId,
                        dinProfil.email,
                        dinProfil.fornavn,
                        dinProfil.etternavn,
                        dinProfil.fdato,
                        bildeURL,
                        textView_privat_bioTekst.text.toString()
                    )
                    database.addBruker(auth.uid.toString(), profil)
                }
            }
            Toast.makeText(context, "Profil er oppdatert", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(){
        val profilListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("Bruker", snapshot.exists().toString())
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
                    button_privat_folgere.text = joined.toString() + " følger"
                else
                    button_privat_folgere.text = joined.toString() + " følgere"
            }

            override fun onCancelled(error: DatabaseError) {
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

    override fun onItemClick(position: Int) {
        val action = NavGraphDirections.actionGlobalArrangementFragment(påmeldteArrangement[position].arrangementId!!,påmeldteArrangement[position].tittel!! )
        findNavController().navigate(action)
    }
}