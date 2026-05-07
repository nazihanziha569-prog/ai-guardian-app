import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapHistoryScreen(userId: String) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    var points by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("LocationHistory")
            .document(userId)
            .collection("points")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                points = result.documents.mapNotNull {
                    val lat = it.getDouble("lat")
                    val lng = it.getDouble("lng")
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                }
                isLoading = false
            }
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        points.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun trajet enregistré 📍")
        }

        else -> {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(points.last(), 14f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {

                // start
                Marker(
                    state = MarkerState(points.first()),
                    title = "Start"
                )

                // end
                Marker(
                    state = MarkerState(points.last()),
                    title = "End"
                )

                // path
                Polyline(
                    points = points,
                    color = Color.Blue,
                    width = 8f
                )
            }
        }
    }
}