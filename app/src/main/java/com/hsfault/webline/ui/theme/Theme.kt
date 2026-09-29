package com.hsfault.webline.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.R

object Hud {
    val Void = Color(0xFF050203)
    val Night = Color(0xFF0E0709)
    val Maroon = Color(0xFF2A0412)
    val Crimson = Color(0xFFD81E4F)
    val Glow = Color(0xFFFF3B6B)
    val Silver = Color(0xFF8A8A92)
    val Light = Color(0xFFEDE6E8)
    val Muted = Color(0xFF8C7E83)
}

val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
)

private val softShadow = Shadow(
    color = Color.Black.copy(alpha = 0.8f),
    offset = Offset(0f, 1f),
    blurRadius = 6f,
)

object HudType {
    val clock = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold,
        fontSize = 68.sp, letterSpacing = 2.sp, color = Hud.Light, shadow = softShadow,
    )
    val title = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, letterSpacing = 6.sp, color = Hud.Light,
    )
    val label = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 2.5.sp, color = Hud.Muted, shadow = softShadow,
    )
    val appName = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.4.sp, color = Hud.Light, shadow = softShadow,
    )
    val body = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, letterSpacing = 0.5.sp, color = Hud.Light,
    )
    val action = TextStyle(
        fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, letterSpacing = 2.sp, color = Hud.Light,
    )
}

/** Tints any app icon toward crimson while keeping ~45% of its original colour. */
val HudIconTint = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.6315f, 0.3575f, 0.066f, 0f, 6f,
            0.044f, 0.5435f, 0.0165f, 0f, 0f,
            0.0605f, 0.1155f, 0.472f, 0f, 3f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

/** Cut-corner HUD outline: top-left and bottom-right corners are sliced off. */
fun hudFramePath(size: Size, cut: Float): Path = Path().apply {
    moveTo(cut, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    lineTo(0f, cut)
    close()
}

/** Draws the HUD frame (fill + 1px stroke + crimson corner ticks) behind the content. */
fun Modifier.hudFrame(
    cut: Dp = 10.dp,
    fill: Brush? = SolidColor(Hud.Night.copy(alpha = 0.88f)),
    stroke: Color = Hud.Silver.copy(alpha = 0.35f),
    accent: Color = Hud.Crimson,
): Modifier = drawBehind {
    val path = hudFramePath(size, cut.toPx())
    if (fill != null) drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 1.dp.toPx()))

    val tick = minOf(14.dp.toPx(), size.minDimension * 0.3f)
    val w = 2.dp.toPx()
    drawLine(accent, Offset(size.width - tick, 0f), Offset(size.width, 0f), w)
    drawLine(accent, Offset(size.width, 0f), Offset(size.width, tick), w)
    drawLine(accent, Offset(0f, size.height - tick), Offset(0f, size.height), w)
    drawLine(accent, Offset(0f, size.height), Offset(tick, size.height), w)
}