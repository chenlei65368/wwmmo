package au.com.codeka.warworlds.client.game.solarsystem

import android.text.SpannableStringBuilder
import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.build.BuildScreen
import au.com.codeka.warworlds.client.game.fleets.FleetsScreen
import au.com.codeka.warworlds.client.game.starsearch.StarRecentHistoryManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.SharedViews
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.client.util.Callback
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Star
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A screen which shows a view of the solar system (star, planets, etc) and is the launching point
 * for managing builds, planet focus, launching fleets and so on.
 */
class SolarSystemScreen(private var star: Star, private val planetIndex: Int) : Screen() {
  private lateinit var context: ScreenContext
  private lateinit var layout: SolarSystemLayout
  private var isCreated = false

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    isCreated = true
    this.context = context
    layout = SolarSystemLayout(context.activity, layoutCallbacks, star, planetIndex)
    App.taskRunner.run(Threads.BACKGROUND, 100.milliseconds) { doRefresh() }
    App.eventBus.register(eventHandler)
  }

  override fun onShow(): ShowInfo {
    StarRecentHistoryManager.addToLastStars(star)
    return builder().view(layout).build()
  }

  override fun onDestroy() {
    isCreated = false
    App.eventBus.unregister(eventHandler)
  }

  /* TODO: redraw callback */
  override val title: CharSequence
    get() {
      val ssb = SpannableStringBuilder()
      ssb.append("○ ")
      ssb.append(star.name)
      ImageHelper.bindStarIcon(
          ssb, 0, 1, context.activity, star, 24, object : Callback<SpannableStringBuilder> {
            override fun run(param: SpannableStringBuilder) {
              // TODO: handle this
            }
          })
      return ssb
    }

  private fun refreshStar(star: Star) {
    layout.refreshStar(star)
    this.star = star
  }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onStarUpdated(s: Star) {
      if (star.id == s.id) {
        refreshStar(s)
      }
    }
  }

  /**
   * Called on a background thread, we'll simulate the star so that it gets update with correct
   * energy, minerals, etc. We'll schedule it to run every 5 seconds we're on this screen.
   */
  private fun doRefresh() {
    StarManager.simulateStarSync(star)
    if (isCreated) {
      App.taskRunner.run(Threads.BACKGROUND, 5.seconds) { doRefresh() }
    }
  }

  private val layoutCallbacks: SolarSystemLayout.Callbacks = object : SolarSystemLayout.Callbacks {
    override fun onBuildClick(planetIndex: Int) {
      context.pushScreen(
          BuildScreen(star, planetIndex),
          SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build())
    }

    override fun onFocusClick(planetIndex: Int) {
      log.info("focus click: %d", planetIndex)
      context.pushScreen(
          PlanetDetailsScreen(star, star.planets[planetIndex]),
          SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build())
    }

    override fun onSitrepClick() {}
    override fun onViewColonyClick(planetIndex: Int) {
      context.pushScreen(
          PlanetDetailsScreen(star, star.planets[planetIndex]),
          SharedViews.Builder()
              .addSharedView(layout.getPlanetView(planetIndex), R.id.planet_icon)
              .addSharedView(R.id.bottom_pane)
              .build())
    }

    override fun onFleetClick(fleetId: Long) {
      context.pushScreen(
          FleetsScreen(star, fleetId),
          SharedViews.Builder()
              .addSharedView(R.id.bottom_pane)
              .build())
    }
  }

  companion object {
    private val log = Log("SolarSystemScreen")
  }

}