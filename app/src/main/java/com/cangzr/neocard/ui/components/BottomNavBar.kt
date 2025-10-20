package com.cangzr.neocard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: Int
)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(
            name = "Ana Sayfa",
            route = Screen.Home.route,
            icon = R.drawable.home
        ),
        BottomNavItem(
            name = "Bağlantılarım",
            route = Screen.Business.route,
            icon = R.drawable.connect
        ),
        BottomNavItem(
            name = "Kart Oluştur",
            route = Screen.CreateCard.route,
            icon = R.drawable.add_card
        ),
        BottomNavItem(
            name = "Profil",
            route = Screen.Profile.route,
            icon = R.drawable.profile
        )
    )

    NavigationBar(
        containerColor = Color(0xFF121212), // koyu arka plan
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.name,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) Color.White else Color.Gray
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp)) // ikon ile çizgi arası boşluk
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(18.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        }
                    }
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
