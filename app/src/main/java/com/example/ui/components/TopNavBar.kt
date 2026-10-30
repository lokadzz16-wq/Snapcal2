package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalLanguage
import com.example.i18n.SnapLanguage
import com.example.i18n.t
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapForeground
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary
import com.example.ui.theme.SnapPrimaryForeground
import com.example.ui.theme.SnapPrimarySoft
import com.google.firebase.auth.FirebaseUser

@Composable
fun TopNavBar(
    currentUser: FirebaseUser?,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onLanguageChange: (SnapLanguage) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang = LocalLanguage.current
    var langMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = SnapCardBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected("snap") }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SnapPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = SnapPrimaryForeground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SnapCal",
                    fontFamily = FrauncesFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnapForeground
                )
            }

            // Right side: Language selector + Auth action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Language Dropdown
                Box {
                    TextButton(
                        onClick = { langMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = t("nav.language"),
                            modifier = Modifier.size(16.dp),
                            tint = SnapMutedForeground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLang.nativeName,
                            fontSize = 12.sp,
                            color = SnapForeground,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false }
                    ) {
                        SnapLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang.nativeName,
                                        fontWeight = if (lang == currentLang) FontWeight.Bold else FontWeight.Normal,
                                        color = if (lang == currentLang) SnapPrimary else SnapForeground
                                    )
                                },
                                onClick = {
                                    langMenuExpanded = false
                                    onLanguageChange(lang)
                                }
                            )
                        }
                    }
                }

                // Auth Action
                if (currentUser != null) {
                    TextButton(
                        onClick = onSignOutClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = t("nav.signOut"),
                            modifier = Modifier.size(16.dp),
                            tint = SnapMutedForeground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t("nav.signOut"),
                            fontSize = 12.sp,
                            color = SnapMutedForeground
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onSignInClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SnapForeground
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SnapCardBorder)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SnapForeground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t("nav.signIn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
