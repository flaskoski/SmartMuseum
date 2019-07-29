package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import flaskoski.rs.smartmuseum.model.*
//import javax.inject.Inject

class ConfigurationsDAO /*@Inject constructor*/(val db: FirebaseFirestore = FirebaseFirestore.getInstance()){
    private val TAG = "ConfigurationsDAO"
    fun getUpdates(callback : (configurations : Configurations?)-> Unit) {
        //add items to grid from DB
        db.collection("configurations")
                .document("updates")
                .get()
                .addOnSuccessListener { document->
                    var configurations = document.toObject(Configurations::class.java)
                    callback(configurations)
                }
                .addOnFailureListener { _ ->
                   // Log.w(TAG, "Error getting documents.", exception)
                    throw Exception("Error getting items")
                }
    }
}