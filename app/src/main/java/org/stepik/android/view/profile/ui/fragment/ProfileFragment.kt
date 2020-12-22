package org.stepik.android.view.profile.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.empty_login.*
import kotlinx.android.synthetic.main.error_no_connection_with_button.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.header_profile.*
import org.stepic.droid.R
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.core.ShareHelper
import org.stepic.droid.ui.activities.MainFeedActivity
import org.stepic.droid.ui.activities.contracts.CloseButtonInToolbar
import org.stepic.droid.ui.util.snackbar
import org.stepic.droid.util.commitNow
import org.stepic.droid.util.resolveResourceIdAttribute
import org.stepik.android.model.user.User
import org.stepik.android.presentation.profile.ProfilePresenter
import org.stepik.android.presentation.profile.ProfileView
import org.stepik.android.view.base.ui.extension.ColorExtensions
import org.stepik.android.view.glide.ui.extension.GlideImageViewWrapper
import org.stepik.android.view.glide.ui.extension.wrapWithGlide
import org.stepik.android.view.injection.profile.ProfileComponent
import org.stepik.android.view.profile.ui.activity.ProfileActivity
import org.stepik.android.view.profile.ui.animation.ProfileHeaderAnimationDelegate
import org.stepik.android.view.profile.ui.delegate.ProfileStatsDelegate
import org.stepik.android.view.profile_achievements.ui.fragment.ProfileAchievementsFragment
import org.stepik.android.view.profile_activities.ui.fragment.ProfileActivitiesFragment
import org.stepik.android.view.profile_certificates.ui.fragment.ProfileCertificatesFragment
import org.stepik.android.view.profile_courses.ui.fragment.ProfileCoursesFragment
import org.stepik.android.view.profile_detail.ui.fragment.ProfileDetailFragment
import org.stepik.android.view.profile_id.ui.fragment.ProfileIdFragment
import org.stepik.android.view.profile_links.ui.fragment.ProfileLinksFragment
import org.stepik.android.view.profile_notification.ui.fragment.ProfileNotificationFragment
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.view.base.ui.extension.argument
import javax.inject.Inject

class ProfileFragment : Fragment(R.layout.fragment_profile), ProfileView {
    companion object {
        const val TAG = "ProfileFragment"

        fun newInstance(): Fragment =
            newInstance(0)

        fun newInstance(userId: Long): Fragment =
            ProfileFragment()
                .apply {
                    this.userId = userId
                }
    }

    @Inject
    internal lateinit var analytic: Analytic

    @Inject
    internal lateinit var shareHelper: ShareHelper

    @Inject
    internal lateinit var screenManager: ScreenManager

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private var userId by argument<Long>()

    private lateinit var profileComponent: ProfileComponent
    private val profilePresenter: ProfilePresenter by viewModels { viewModelFactory }

    private lateinit var viewStateDelegate: ViewStateDelegate<ProfileView.State>

    private lateinit var profileStatsDelegate: ProfileStatsDelegate
    private lateinit var headerAnimationDelegate: ProfileHeaderAnimationDelegate

    private lateinit var profileImageWrapper: GlideImageViewWrapper

    private var shareMenuItem: MenuItem? = null
    private var isShareMenuItemVisible: Boolean = false
        set(value) {
            field = value
            shareMenuItem?.isVisible = value
        }

    private var editMenuItem: MenuItem? = null
    private var isEditMenuItemVisible: Boolean = false
        set(value) {
            field = value
            editMenuItem?.isVisible = value
        }

    private var settingsMenuItem: MenuItem? = null
    private var isSettingsMenuItemVisible: Boolean = false
        set(value) {
            field = value
            settingsMenuItem?.isVisible = value
        }

    private var menuTintStateList: ColorStateList = ColorStateList.valueOf(0x0)
        set(value) {
            field = value

            toolbar?.navigationIcon?.let { DrawableCompat.setTintList(it, value) }
            editMenuItem?.let { MenuItemCompat.setIconTintList(it, value) }
            shareMenuItem?.let { MenuItemCompat.setIconTintList(it, value) }
            settingsMenuItem?.let { MenuItemCompat.setIconTintList(it, value) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectComponent()

        super.onCreate(savedInstanceState)
        profilePresenter.onData(userId)
    }

    private fun injectComponent() {
        profileComponent = App
            .componentManager()
            .profileComponent(userId)
        profileComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<ProfileView.State.Idle>()
        viewStateDelegate.addState<ProfileView.State.Loading>(profileLoading)
        viewStateDelegate.addState<ProfileView.State.Content>(scrollContainer)
        viewStateDelegate.addState<ProfileView.State.Empty>(profileEmpty)
        viewStateDelegate.addState<ProfileView.State.EmptyLogin>(profileEmptyLogin)
        viewStateDelegate.addState<ProfileView.State.NetworkError>(profileNetworkError)

        profileStatsDelegate = ProfileStatsDelegate(view, analytic)

        if (activity is CloseButtonInToolbar) {
            toolbar.setNavigationIcon(com.google.android.material.R.drawable.abc_ic_ab_back_material)
            toolbar.navigationIcon?.let { DrawableCompat.setTintList(it, menuTintStateList) }
            toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        }
        ViewCompat.setElevation(header, resources.getDimension(R.dimen.profile_header_elevation))

        toolbar.inflateMenu(R.menu.profile_menu)
        initOptionsMenu(toolbar.menu)
        toolbar.setOnMenuItemClickListener(::onOptionsItemClicked)

        val colorControlNormal =
            AppCompatResources.getColorStateList(requireContext(), requireContext().resolveResourceIdAttribute(R.attr.colorControlNormal))

        headerAnimationDelegate =
            ProfileHeaderAnimationDelegate(
                view,
                menuColorStart = ContextCompat.getColor(requireContext(), R.color.white),
                menuColorEnd = colorControlNormal?.defaultColor ?: 0x0,
                toolbarColor = ColorExtensions.colorSurfaceWithElevationOverlay(requireContext(), 4)
            ) { menuTintStateList = it }

        scrollContainer
            .setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
                headerAnimationDelegate.onScroll(scrollY)
            }
        view.doOnNextLayout { headerAnimationDelegate.onScroll(scrollContainer.scrollY) }

        tryAgain.setOnClickListener { profilePresenter.onData(userId, forceUpdate = true) }
        authAction.setOnClickListener { screenManager.showLaunchScreen(context, true, MainFeedActivity.PROFILE_INDEX) }

        profileImageWrapper = profileImage.wrapWithGlide()

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.container, ProfileNotificationFragment.newInstance(userId))
                add(R.id.container, ProfileCoursesFragment.newInstance(userId))
                add(R.id.container, ProfileActivitiesFragment.newInstance(userId))
                add(R.id.container, ProfileAchievementsFragment.newInstance(userId))
                add(R.id.container, ProfileCertificatesFragment.newInstance(userId))
                add(R.id.container, ProfileLinksFragment.newInstance(userId))
                add(R.id.container, ProfileDetailFragment.newInstance(userId))
                add(R.id.container, ProfileIdFragment.newInstance(userId))
            }
        }

        if (activity is ProfileActivity) {
            view.setBackgroundColor(0x0)
        }
    }

    private fun initOptionsMenu(menu: Menu) {
        editMenuItem = menu.findItem(R.id.menu_item_edit)
        editMenuItem?.isVisible = isEditMenuItemVisible
        editMenuItem?.let { MenuItemCompat.setIconTintList(it, menuTintStateList) }

        shareMenuItem = menu.findItem(R.id.menu_item_share)
        shareMenuItem?.isVisible = isShareMenuItemVisible
        shareMenuItem?.let { MenuItemCompat.setIconTintList(it, menuTintStateList) }

        settingsMenuItem = menu.findItem(R.id.menu_item_settings)
        settingsMenuItem?.isVisible = isSettingsMenuItemVisible
        settingsMenuItem?.let { MenuItemCompat.setIconTintList(it, menuTintStateList) }
    }

    private fun onOptionsItemClicked(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_item_edit -> {
                analytic.reportAmplitudeEvent(AmplitudeAnalytic.ProfileEdit.SCREEN_OPENED)
                screenManager.showProfileEdit(context)
                true
            }

            R.id.menu_item_share -> {
                profilePresenter.onShareProfileClicked()
                true
            }

            R.id.menu_item_settings -> {
                analytic.reportEvent(Analytic.Screens.USER_OPEN_SETTINGS)
                screenManager.showSettings(activity)
                true
            }

            else ->
                super.onOptionsItemSelected(item)
        }

    override fun onStart() {
        super.onStart()
        profilePresenter.attachView(this)
    }

    override fun onStop() {
        profilePresenter.detachView(this)
        super.onStop()
    }

    override fun setState(state: ProfileView.State) {
        viewStateDelegate.switchState(state)

        when (state) {
            is ProfileView.State.Content -> {
                with(state.profileData) {
                    profileImageWrapper
                        .setImagePath(
                            user.avatar ?: "",
                            AppCompatResources.getDrawable(requireContext(), R.drawable.general_placeholder)
                        )

                    profileName.text = user.fullName
                    profileBio.text = user.shortBio
                    profileBio.isVisible = !user.shortBio.isNullOrBlank()

                    toolbarTitle.text = user.fullName
                    toolbarTitle.translationY = 1000f

                    isEditMenuItemVisible = isCurrentUser
                    isShareMenuItemVisible = true
                    isSettingsMenuItemVisible = isCurrentUser

                    profileStatsDelegate.setProfileStats(user)

                    profileCover.isVisible = !user.cover.isNullOrEmpty()
                    Glide
                        .with(requireContext())
                        .asBitmap()
                        .centerCrop()
                        .load(user.cover)
                        .into(profileCover)

                    view?.doOnNextLayout { headerAnimationDelegate.onScroll(scrollContainer.scrollY) }
                }
            }

            else -> {
                toolbarTitle.setText(R.string.profile_title)
                toolbarTitle.translationY = 0f

                isEditMenuItemVisible = false
                isShareMenuItemVisible = false
                isSettingsMenuItemVisible = false
            }
        }
    }

    override fun showNetworkError() {
        view?.snackbar(messageRes = R.string.connectionProblems)
    }

    override fun shareUser(user: User) {
        startActivity(shareHelper.getIntentForUserSharing(user))
    }
}