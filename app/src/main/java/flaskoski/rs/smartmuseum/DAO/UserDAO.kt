package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.User

class UserDAO//(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
//    private val TAG = "RatingDAO"
//    private var name : String = ""
//
//    fun add(user: User) {
//        db.collection("ratings")
//                .whereEqualTo("id", user.id)
//                .whereEqualTo("recommendationSystem", user.recommendationSystem)
//                .get().addOnSuccessListener { userList ->
//                    removeName(user)
//                    if (userList.isEmpty) {
//                        db.collection("users").add(user)
//                        Log.i(TAG, "User id added on db: $user")
//                    } else
//                        for (user in userList) {
//                            user.reference.set(user)
//                            Log.i(TAG, "User info changed on db: $user")
//                        }
//                    restoreName(user)
//                }
//                .addOnFailureListener { e ->
//                    Log.w(TAG, "Error adding document", e)
//                    //TODO offline mode with sync button
//                }
//    }
//
//    private fun restoreName(user: User) {
//        user.name = name
//    }
//
//    private fun removeName(user: User) {
//        name = user.name
//        user.name = ""
//    }
//
//}