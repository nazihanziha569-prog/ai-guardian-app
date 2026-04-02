package com.example.ai_guardian

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ai_guardian.ui.theme.AI_guardianTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.integration.android.*
import android.content.*
import androidx.activity.compose.rememberLauncherForActivityResult


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var screen by remember { mutableStateOf("splash") }

            when (screen) {

                "splash" -> SplashScreen {
                    screen = "login"
                }

                "login" -> LoginScreen(
                    onRegisterClick = { screen = "register" },
                    onLoginSuccess = { screen = "dashboard" },
                    onScanClick = { screen = "qr_scan" }
                )


                "register" -> RegisterScreen(
                    onLoginClick = { screen = "login" }
                )

                "dashboard" -> DashboardScreen(
                    onQrClick = { screen = "qr" }
                )

                "qr" -> GenerateQRScreen(
                    onBack = { screen = "dashboard" }
                )
                "qr_scan" -> QRScreen(
                    onBack = { screen = "login" }
                )
            }
        }
    } }
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AI_guardianTheme {
        Greeting("Android")
    }
}
@Composable
fun SplashScreen(onFinish: () -> Unit) {

    LaunchedEffect(true) {
        delay(3000) // 3 seconds
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            CircularProgressIndicator()
        }
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(text = "Welcome to the App")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSplash() {
    SplashScreen {}
}

    @Composable
    fun RegisterScreen(onLoginClick: () -> Unit) {

        val context = LocalContext.current

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "logo",
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "AI Guardian",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Protéger et accompagner chaque jour"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Créer un compte",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter votre nom") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter votre email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Choisir un mot de pass") },
                modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Cacher mot de passe" else "Voir mot de passe")
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {

                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Remplir tous les champs ❌", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {

                            val uid = auth.currentUser!!.uid

                            val user = hashMapOf(
                                "nom" to name,
                                "email" to email,
                                "role" to "surveillee"
                            )

                            db.collection("Users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Compte créé ✅", Toast.LENGTH_SHORT).show()

                                    // 🔥 يمشي للـ login
                                    onLoginClick()
                                }

                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Erreur: ${it.message}", Toast.LENGTH_LONG).show()
                        }

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("S’inscrire")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Vous avez déjà un compte")

            Text(
                text = "Se connecter",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    onLoginClick()
                }
            )
        }
    }
@Composable
fun LoginScreen(onRegisterClick: () -> Unit,
                onLoginSuccess: () -> Unit,
                onScanClick: () -> Unit) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Se connecter",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Default.Visibility
                else
                    Icons.Default.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (passwordVisible) "Cacher" else "Voir"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Mot de passe oublié ?",
            color = Color.Blue,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Remplir tous les champs ❌", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Login réussi ✅", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erreur: ${it.message}", Toast.LENGTH_LONG).show()
                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se connecter")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                onScanClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scanner QR du superviseur")
        }

        Row {

            Text("Vous n'avez pas de compte ? ")

            Text(
                text = "Créer un compte",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    onRegisterClick()
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(onQrClick: () -> Unit) {

    var selectedScreen by remember { mutableStateOf("home") }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔹 CONTENT حسب الصفحة
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
                .padding(16.dp)
        ) {

            when (selectedScreen) {
                "home" -> HomeContent()
                "alerts" -> AlertsScreen()
                "settings" -> SettingsScreen()
                "history" -> HistoryScreen()
            }
        }

        // 🔹 NAV BAR
        BottomNavBar(
            selected = selectedScreen,
            onItemSelected = { selectedScreen = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FloatingActionButton(
            onClick = { onQrClick() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-35).dp),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.QrCode, contentDescription = "QR", tint = Color.Blue)
        }
    }
}



@Composable
fun BottomNavBar(
    selected: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        NavItem("📋", "Home", selected == "home") {
            onItemSelected("home")
        }

        NavItem("🔔", "Alerts", selected == "alerts") {
            onItemSelected("alerts")
        }

        NavItem("⚙️", "Settings", selected == "settings") {
            onItemSelected("settings")
        }

        NavItem("🕘", "History", selected == "history") {
            onItemSelected("history")
        }
    }}


    @Composable
    fun NavItem(
        icon: String,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {

        val color = if (isSelected) Color(0xFF1976D2) else Color.Gray

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onClick() }
        ) {
            Text(icon, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = color)
        }
    }

@Composable
fun HomeContent() {
    Text("Home Screen", fontSize = 22.sp)
}

@Composable
fun AlertsScreen() {
    Text("Alerts Screen", fontSize = 22.sp)
}

@Composable
fun SettingsScreen() {
    Text("Settings Screen", fontSize = 22.sp)
}

@Composable
fun HistoryScreen() {
    Text("History Screen", fontSize = 22.sp)
}

    @Composable
    fun UserCard(name: String, status: String, isSafe: Boolean) {

        val bgColor = if (isSafe) Color(0xFFDFF5E1) else Color(0xFFFDE0E0)
        val textColor = if (isSafe) Color(0xFF2E7D32) else Color(0xFFC62828)

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp)
            ) {

                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Status: $status",
                    color = textColor,
                    modifier = Modifier
                        .background(bgColor)
                        .padding(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir localisation")
                }
            }
        }}

@Composable
fun QRScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val activity = context as Activity

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val intentResult = IntentIntegrator.parseActivityResult(
            result.resultCode,
            result.data
        )

        if (intentResult != null && intentResult.contents != null) {

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            val superviseeId = auth.currentUser?.uid
            val superviseurId = intentResult.contents

            if (superviseeId != null && superviseurId != null) {

                val relation = hashMapOf(
                    "superviseurId" to superviseurId,
                    "superviseeId" to superviseeId
                )

                db.collection("Associations")
                    .add(relation)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Linked successfully ✅", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    LaunchedEffect(Unit) {

        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR Code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)

        launcher.launch(integrator.createScanIntent())
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Ouverture de la caméra...")
    }
}

@Composable
fun GenerateQRScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: "no_user"

    val qrData = uid

    val bitmap = remember {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)

        val bmp = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.RGB_565)

        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {


            Spacer(modifier = Modifier.height(20.dp))

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(250.dp)
            )
        }
    }
}

