package org.stepic.droid.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;
import org.stepic.droid.R;
import org.stepic.droid.analytic.AmplitudeAnalytic;
import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.base.App;
import org.stepic.droid.base.FragmentBase;
import org.stepic.droid.model.NotificationCategory;
import org.stepic.droid.ui.activities.MainFeedActivity;

import timber.log.Timber;

public class NotificationsFragment extends FragmentBase {

    public static final String TAG = "NotificationsFragment";

    @NotNull
    public static NotificationsFragment newInstance() {
        Bundle args = new Bundle();
        NotificationsFragment fragment = new NotificationsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Button authUserButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        analytic.reportAmplitudeEvent(AmplitudeAnalytic.Notifications.NOTIFICATION_SCREEN_OPENED);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = view.findViewById(R.id.notification_tabs);
        viewPager = view.findViewById(R.id.notification_viewpager);
        final View needAuthRootView = view.findViewById(R.id.needAuthView);
        authUserButton = view.findViewById(R.id.authAction);

        if (getSharedPreferenceHelper().getAuthResponseFromStore() == null) {
//            authUserButton.setOnClickListener(v -> getScreenManager().showLaunchScreen(getActivity()));
            authUserButton.setOnClickListener(v -> getScreenManager().showLaunchScreen(getActivity(), true, MainFeedActivity.NOTIFICATIONS_INDEX));
//            toolbar.setVisibility(View.GONE); // FIXME: 15.08.17 hide, when it is needed
            initToolbar();
            tabLayout.setVisibility(View.GONE);
            needAuthRootView.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
            initToolbar();
//            toolbar.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            needAuthRootView.setVisibility(View.GONE);
            initViewPager();
        }
    }

    private void initViewPager() {
        viewPager.setAdapter(new NotificationPagerAdapter(getChildFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onDestroyView() {
        authUserButton.setOnClickListener(null);
        super.onDestroyView();
    }

    private void initToolbar() {
        final TextView toolbarTitle = getView().findViewById(R.id.centeredToolbarTitle);
        toolbarTitle.setText(R.string.notification_title);
        final Toolbar toolbar = getView().findViewById(R.id.centeredToolbar);
        toolbar.inflateMenu(R.menu.notification_center_menu);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            getAnalytic().reportEvent(Analytic.Interaction.CLICK_SETTINGS_FROM_NOTIFICATION);
            getScreenManager().showNotificationSettings(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class NotificationPagerAdapter extends FragmentStatePagerAdapter {
        private final int numberOfCategories = NotificationCategory.values().length;

        public NotificationPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            Timber.d("getItem %d", position);
            return NotificationListFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return numberOfCategories;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Timber.d("getPageTitle %d", position);
            int resString = NotificationCategory.values()[position].getTitle();
            return App.Companion.getAppContext().getString(resString);
        }
    }

}
