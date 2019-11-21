package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.User
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class UserDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"
    private var age : String = ""

    fun add(userToBeAdded: User) {
        db.collection("users")
                .whereEqualTo("id", userToBeAdded.id)
                .whereEqualTo(User.FIELD_AGE, userToBeAdded.getAgeGroup())
                .get().addOnSuccessListener { userList ->
                    if (userList.isEmpty) {
                        db.collection("users").add(userToBeAdded)
                        Log.i(TAG, "User id added on db: $userToBeAdded")
                    } else
                        for (user in userList) {
                            user.reference.set(userToBeAdded)
                            Log.i(TAG, "User info changed on db: $userToBeAdded")
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
}