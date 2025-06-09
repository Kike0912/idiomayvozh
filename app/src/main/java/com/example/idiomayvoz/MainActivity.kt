package com.example.idiomayvoz

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idiomayvoz.ui.theme.IdiomayVozTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var ttsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.getDefault())
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        setContent {
            IdiomayVozTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (ttsReady) {
                        MainScreen(tts)
                    } else {
                        // Mostrar mensaje o pantalla de carga mientras TTS está listo
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Inicializando TextToSpeech...")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun MainScreen(tts: TextToSpeech) {
    var texto by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(Locale.getDefault()) }
    var selectedVoice by remember { mutableStateOf<Voice?>(null) }
    var availableLanguages by remember { mutableStateOf(emptyList<Locale>()) }
    var availableVoices by remember { mutableStateOf(emptyList<Voice>()) }

    var languageMenuExpanded by remember { mutableStateOf(false) }
    var voiceMenuExpanded by remember { mutableStateOf(false) }

    // Actualizar idiomas y voces cuando cambia TTS o el idioma seleccionado
    LaunchedEffect(tts) {
        availableLanguages = try {
            tts.availableLanguages?.toList()?.sortedBy { it.displayLanguage } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        availableVoices = try {
            tts.voices.filter { it.locale.language == selectedLanguage.language }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }

        if (selectedVoice == null || selectedVoice?.locale?.language != selectedLanguage.language) {
            selectedVoice = availableVoices.firstOrNull()
        }
    }

    // Actualizar voces cuando cambia el idioma
    LaunchedEffect(selectedLanguage) {
        availableVoices = try {
            tts.voices.filter { it.locale.language == selectedLanguage.language }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
        selectedVoice = availableVoices.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "App TRONCO-MEGAFONO",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Ingresar Texto a Publicar") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        // Selector de idioma
        Box {
            Button(
                onClick = { languageMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Idioma: ${selectedLanguage.displayLanguage}")
            }
            DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .wrapContentWidth()  // Ajusta el tamaño al contenido
            ) {
                availableLanguages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayLanguage) },
                        onClick = {
                            selectedLanguage = lang
                            val result = tts.setLanguage(lang)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                // Manejar error, quizás mostrar Toast
                            }
                            languageMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Selector de voz
        Box {
            Button(
                onClick = { voiceMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Voz: ${selectedVoice?.name ?: "Seleccionar"}")
            }
            DropdownMenu(
                expanded = voiceMenuExpanded,
                onDismissRequest = { voiceMenuExpanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .wrapContentWidth()
            ) {
                availableVoices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.name) },
                        onClick = {
                            selectedVoice = voice
                            tts.voice = voice
                            voiceMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Botón Hablar
        Button(
            onClick = {
                if (texto.isNotBlank() && selectedVoice != null) {
                    tts.voice = selectedVoice
                    tts.language = selectedLanguage
                    tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts1")
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text(text = "Habla", fontSize = 18.sp)
        }
    }
}
