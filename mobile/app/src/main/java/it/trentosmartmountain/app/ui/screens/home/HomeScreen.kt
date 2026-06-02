package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/**
 * Tab **Home**: due sotto-tab interni (navigazione locale, non Jetpack Navigation).
 *
 * - Sociale — feed community ([HomeSocialScreen])
 * - Personale — storico attività ([ActivityListScreen])
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  onActivityClick: (activityId: String, sessionId: String?) -> Unit = { _, _ -> },
  onNavigateToUserProfile: (userId: String) -> Unit = {},
  onNavigateToSessionDetail: (sessionId: String) -> Unit = {},
  onNavigateToStoryViewer: (refId: String, kind: String) -> Unit = { _, _ -> },
  onNavigateToUserSearch: () -> Unit = {},
  onNavigateToLeaderboard: () -> Unit = {},
  onNavigateToNotifications: () -> Unit = {},
  onNavigateToPostDetail: (item: it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit = {},
) {
  var subTab by rememberSaveable { mutableIntStateOf(0) }

  Column(modifier = modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = subTab) {
      Tab(
        selected = subTab == 0,
        onClick = { subTab = 0 },
        text = { Text(stringResource(R.string.home_tab_social)) },
      )
      Tab(
        selected = subTab == 1,
        onClick = { subTab = 1 },
        text = { Text(stringResource(R.string.home_tab_personal)) },
      )
    }
    when (subTab) {
      0 -> HomeSocialScreen(
        onUserClick = onNavigateToUserProfile,
        onLiveClick = onNavigateToSessionDetail,
        onStoryClick = onNavigateToStoryViewer,
        onSearchClick = onNavigateToUserSearch,
        onLeaderboardClick = onNavigateToLeaderboard,
        onNotificationsClick = onNavigateToNotifications,
        onOpenDetail = onNavigateToPostDetail,
      )
      1 -> ActivityListScreen(onActivityClick = onActivityClick)
    }
  }
}
