package com.example.greenbook.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.greenbook.Database
import com.example.greenbook.R
import com.example.greenbook.adaptorClasses.InnleggAdaptor
import com.example.greenbook.adaptorClasses.PostAdaptor
import com.example.greenbook.dataObjekter.Arrangement
import com.example.greenbook.dataObjekter.Innlegg
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class ArrangementFragment : Fragment(R.layout.fragment_arrangement), InnleggAdaptor.OnItemClickListener{

    private val args: ArrangementFragmentArgs by navArgs()
    private lateinit var database: Database
    private lateinit var arrangement: Arrangement
    private lateinit var user: FirebaseUser

    private lateinit var tittel: TextView
    private lateinit var beskrivelse: TextView
    private lateinit var btn_påmeldte: Button
    private lateinit var btn_join: Button
    private lateinit var btn_skrivInlegg: Button

    var innlegg: ArrayList<Innlegg> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Database()
        user = Firebase.auth.currentUser!!

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tittel = view?.findViewById(R.id.arrangement_tittel)
        beskrivelse = view?.findViewById(R.id.arrangement_txt_beskrivelse)
        btn_påmeldte = view?.findViewById(R.id.arrangement_påmeldte)
        btn_join = view?.findViewById(R.id.arrangement_btn_blimed)
        btn_skrivInlegg = view?.findViewById(R.id.arrangement_btn_skriv_innlegg)


        updateUI()

        btn_skrivInlegg.setOnClickListener{
            val action = ArrangementFragmentDirections.actionArrangementFragmentToSkrivInnleggFragment(args.arrangementID)
            findNavController().navigate(action)
        }

        btn_join.setOnClickListener {
            if(btn_join.text.equals("Bli med")){

                database.meldBrukerPåArrangement(user.uid, args.arrangementID)
            }
            else{
                database.meldBrukerAvArrangement(user.uid, args.arrangementID)
            }
        }
    }
    fun hentInnlegg(){
        innlegg = ArrayList()
        val arrangementListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(arr in snapshot.children){
                    val arra= arr.getValue<Innlegg>()
                    innlegg.add(arra!!)
                }
                update(innlegg)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        database.database.child("innlegg").addValueEventListener(arrangementListener)
    }
    private fun update(arr: ArrayList<Innlegg>){
        val recyclerView = getView()?.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = InnleggAdaptor(arr, this)
    }
    private fun updateUI(){
        val arrangementListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val arr = snapshot.getValue<Arrangement>()!!
                update(arr!!)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        database.database.child("arrangement").child(args.arrangementID).addValueEventListener(arrangementListener)

        val deltakereListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val joined = snapshot.childrenCount.toInt()
                if(joined == 1)
                    btn_påmeldte.text = joined.toString() + " påmeldt"
                else
                    btn_påmeldte.text = joined.toString() + " påmeldte"

                if(snapshot.hasChild(user.uid))
                    btn_join.text =  "Påmeldt"
                else
                    btn_join.text =  "Bli med"
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }

        database.database.child("påmeldinger").child(args.arrangementID).addValueEventListener(deltakereListener)

    }

    private fun update(arrangement: Arrangement){
        tittel.text = arrangement.tittel
        beskrivelse.text = (
                        "Sted: " + arrangement.sted + "\n"+
                        "Tid: " + arrangement.tid + "\n"+
                        arrangement.beskrivelse
                )
    }

    override fun onItemClick(position: Int) {
        TODO("Not yet implemented")
    }
}

