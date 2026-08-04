package com.mndublo.odolens.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.mndublo.odolens.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MainScreen(onItemClick = {}) }
  }

  @Test
  fun tripsTab_exists() {
    composeTestRule.onNodeWithText("Trips").assertExists()
  }

  @Test
  fun parkingTab_exists() {
    composeTestRule.onNodeWithText("Parking").assertExists()
  }
}
