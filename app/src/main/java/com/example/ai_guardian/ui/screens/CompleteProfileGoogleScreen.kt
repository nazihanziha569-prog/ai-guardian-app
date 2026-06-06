package com.example.ai_guardian.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ai_guardian.R
import com.example.ai_guardian.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CompleteProfileGoogleScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { imageUri = it } }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val displayName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "AI Guardian",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        Text(
            text = "Protéger et accompagner chaque jour",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Compléter votre profil",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Text(
                    "Ajouter une photo",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))



                OutlinedTextField(
                    value = displayName,
                    onValueChange = {},
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (phone.isEmpty() || age.isEmpty()) {
                            Toast.makeText(context, "Remplir tous les champs ❌", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val saveData = {
                            FirebaseFirestore.getInstance()
                                .collection("Users").document(uid)
                                .update(mapOf("phone" to phone, "age" to age))
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener {
                                    Toast.makeText(context, it.message ?: "Erreur", Toast.LENGTH_SHORT).show()
                                }
                        }

                        val uri = imageUri
                        if (uri != null) {
                            Thread {
                                try {
                                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@Thread
                                    val original = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    val scaled = android.graphics.Bitmap.createScaledBitmap(original, 200, 200, true)
                                    val out = java.io.ByteArrayOutputStream()
                                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                                    val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.DEFAULT)
                                    val dataUrl = "data:image/jpeg;base64,$base64"

                                    FirebaseFirestore.getInstance()
                                        .collection("Users").document(uid)
                                        .update("imageUrl", dataUrl)
                                        .addOnSuccessListener { saveData() }
                                } catch (e: Exception) {
                                    saveData()
                                }
                            }.start()
                        } else {
                            saveData()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Continuer", color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Annuler",
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onCancel() }
                )
            }
        }
    }
}