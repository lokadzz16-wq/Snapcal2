package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.t
import com.example.ui.theme.SnapCard
import com.example.ui.theme.SnapCardBorder
import com.example.ui.theme.SnapMutedForeground
import com.example.ui.theme.SnapPrimary

data class TabItem(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun BottomNavTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem("snap", "nav.snap", Icons.Default.CameraAlt, "tab_snap"),
        TabItem("diary", "nav.diary", Icons.Default.Book, "tab_diary"),
        TabItem("insights", "nav.insights", Icons.Default.ShowChart, "tab_insights"),
        TabItem("goals", "nav.goals", Icons.Default.TrackChanges, "tab_goals")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SnapCard)
            .border(width = 1.dp, color = SnapCardBorder)
            .navigationBarsPadding()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab.route
            val color = if (isSelected) SnapPrimary else SnapMutedForeground

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab.route) }
                    .testTag(tab.testTag)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = t(tab.labelKey),
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = t(tab.labelKey),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = color,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
