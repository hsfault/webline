package com.hsfault.webline.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hsfault.webline.R

object Hud {
    val Bg = Color(0xFF060609)
    val Surface = Color(0xFF0E0E12)
    val Line = Color(0xFF26262C)
    val Red = Color(0xFFD01422)
    val Glow = Color(0xFFFF2A2A)
    val DeepRed = Color(0xFF5A0006)
    val White = Color(0xFFF2F2F4)
    val Grey = Color(0xFF9A9AA2)
    val Soft = Color(0xFFC9C9CF)
}

val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/*
 * No text shadows anywhere: Android re-blurs a text shadow on every frame
 * (it can't cache it), which was the main cause of the swipe/scroll lag.
 */
object HudType {
    val clock = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = Hud.White)
    val clockSuffix = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Hud.White)
    val date = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 3.sp, lineHeight = 15.sp, color = Hud.White)
    val tagline = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 8.sp, letterSpacing = 3.sp, color = Hud.Grey)
    val quote = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 4.5.sp, lineHeight = 21.sp, color = Hud.Soft)
    val temp = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Hud.White)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Hud.White)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Hud.White)
    val cardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Hud.White)
    val cardSub = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = Hud.Grey)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Hud.White)
    val menu = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp, color = Hud.White)
    val header = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.5.sp, color = Hud.Red)
    val rail = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = Hud.Red)
}