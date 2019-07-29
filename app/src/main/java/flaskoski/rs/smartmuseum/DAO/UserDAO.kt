package flaskoski.rs.smartmuseum.DAO

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.model.User

class UserDAO(val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val TAG = "RatingDAO"
    private var age : String = ""

    fun add(user: User) {
        db.collection("users")
                .whereEqualTo("id", user.id)
                .whereEqualTo(User.FIELD_AGE, user.getAgeGroup())
                .get().addOnSuccessListener { userList ->
                    if (userList.isEmpty) {
                        db.collection("users").add(user)
                        Log.i(TAG, "User id added on db: $user")
                    } else
                        for (user in userList) {
                            user.reference.set(user)
                            Log.i(TAG, "User info changed on db: $user")
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    //TODO offline mode with sync button
                }
    }
}