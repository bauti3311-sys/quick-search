package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta de colores inspirada en tu HTML (macOS Tahoe & Sonoma)
object SpotlightColors {
    val MacBlue = Color(0xFF007AFF)
    val MacLightBlue = Color(0xFF5AC8FA)
    val MacGreen = Color(0xFF34C759)
    val MacOrange = Color(0xFFFF9500)
    val MacPurple = Color(0xFFAF52DE)
    val MacRed = Color(0xFFFF3B30)
    val MacYellow = Color(0xFFFFCC00)

    // Glass / Frosted Glass
    val PillBackgroundLight = Color(0xFFFFFFFF).copy(alpha = 0.85f)
    val PillBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.90f)
    val PillBackgroundDark = Color(0xFF182030).copy(alpha = 0.88f)
    val PillBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.15f)
}

enum class SpotlightTheme(val displayName: String, val primaryColor: Color) {
    TAHOE("Tahoe Ice", Color(0xFF007AFF)),
    FROST("Sonoma Frost", Color(0xFF5AC8FA)),
    DARK("Midnight Dark", Color(0xFF1E293B)),
    GRAPHITE("Graphite Slate", Color(0xFF64748B)),
    AURORA("Aurora Violet", Color(0xFFAF52DE))
}

@Composable
fun SpotlightPillBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeScope: String?, // "apps", "files", "actions", "clipboard", etc.
    onScopeSelected: (String?) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf(SpotlightTheme.TAHOE) }

    val pillBg = if (isDark) SpotlightColors.PillBackgroundDark else SpotlightColors.PillBackgroundLight
    val pillBorder = if (isDark) SpotlightColors.PillBorderDark else SpotlightColors.PillBorderLight
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val placeholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF8E8E93)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila principal: Cápsula (Pill) + 5 Botones Circulares
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Barra tipo Cápsula Central (Pill)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(50.dp), spotColor = currentTheme.primaryColor.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(50.dp))
                    .background(pillBg)
                    .border(width = 1.dp, color = pillBorder, shape = RoundedCornerShape(50.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de Lupa
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Buscar",
                    tint = placeholderColor,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Chip de Filtro activo dentro de la barra
                if (activeScope != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(currentTheme.primaryColor.copy(alpha = 0.15f))
                            .border(1.dp, currentTheme.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeScope.replaceFirstChar { it.uppercase() },
                            color = currentTheme.primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Quitar filtro",
                            tint = currentTheme.primaryColor,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onScopeSelected(null) }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Input de Texto
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(currentTheme.primaryColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Spotlight Search",
                                color = placeholderColor,
                                fontSize = 17.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Botón Limpiar Texto
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Limpiar",
                            tint = placeholderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Fila de Botones Circulares Laterales (Image 2 de tu HTML)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón 1: Apps [ A ]
                SpotlightCircleButton(
                    icon = Icons.Rounded.Apps,
                    label = "Apps",
                    isActive = activeScope == "apps",
                    activeColor = currentTheme.primaryColor,
                    isDark = isDark,
                    onClick = { onScopeSelected(if (activeScope == "apps") null else "apps") }
                )

                // Botón 2: Archivos [ 📁 ]
                SpotlightCircleButton(
                    icon = Icons.Rounded.Folder,
                    label = "Archivos",
                    isActive = activeScope == "files",
                    activeColor = currentTheme.primaryColor,
                    isDark = isDark,
                    onClick = { onScopeSelected(if (activeScope == "files") null else "files") }
                )

                // Botón 3: Acciones [ 🥞 ]
                SpotlightCircleButton(
                    icon = Icons.Rounded.Layers,
                    label = "Acciones",
                    isActive = activeScope == "actions",
                    activeColor = currentTheme.primaryColor,
                    isDark = isDark,
                    onClick = { onScopeSelected(if (activeScope == "actions") null else "actions") }
                )

                // Botón 4: Portapapeles [ 📄 ]
                SpotlightCircleButton(
                    icon = Icons.Rounded.ContentPaste,
                    label = "Portapapeles",
                    isActive = activeScope == "clipboard",
                    activeColor = currentTheme.primaryColor,
                    isDark = isDark,
                    onClick = { onScopeSelected(if (activeScope == "clipboard") null else "clipboard") }
                )

                // Botón 5: Paleta de Temas [ 🎨 ]
                SpotlightCircleButton(
                    icon = Icons.Rounded.Palette,
                    label = "Tema",
                    isActive = showThemeMenu,
                    activeColor = currentTheme.primaryColor,
                    isDark = isDark,
                    onClick = { showThemeMenu = !showThemeMenu }
                )
            }
        }

        // Selector de tema desplegable cuando se toca el botón de paleta
        AnimatedVisibility(visible = showThemeMenu) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg)
                    .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotlightTheme.values().forEach { theme ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                            .border(
                                width = if (currentTheme == theme) 2.dp else 0.dp,
                                color = if (currentTheme == theme) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                currentTheme = theme
                                showThemeMenu = false
                            }
                    )
                }
            }
        }
    }
}

// Botón circular flotante estilo macOS
@Composable
fun SpotlightCircleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) {
        activeColor.copy(alpha = 0.20f)
    } else {
        if (isDark) SpotlightColors.PillBackgroundDark else SpotlightColors.PillBackgroundLight
    }

    val border = if (isActive) {
        activeColor
    } else {
        if (isDark) SpotlightColors.PillBorderDark else SpotlightColors.PillBorderLight
    }

    val tint = if (isActive) activeColor else (if (isDark) Color.White else Color(0xFF3A3A3C))

    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(bg)
            .border(width = if (isActive) 1.5.dp else 1.dp, color = border, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
