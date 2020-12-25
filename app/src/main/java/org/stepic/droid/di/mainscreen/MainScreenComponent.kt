package org.stepic.droid.di.mainscreen

import dagger.Subcomponent
import org.stepic.droid.ui.activities.MainFeedActivity
import org.stepik.android.view.injection.course.CourseDataModule
import org.stepik.android.view.injection.course_payments.CoursePaymentsDataModule
import org.stepik.android.view.injection.profile.ProfileDataModule
import org.stepik.android.view.injection.user.UserDataModule
import org.stepik.android.view.injection.user_profile.UserProfileDataModule

@MainScreenScope
@Subcomponent(modules = [
    CourseDataModule::class,
    CoursePaymentsDataModule::class,
    UserDataModule::class,
    UserProfileDataModule::class,
    ProfileDataModule::class
])
interface MainScreenComponent {

    @Subcomponent.Builder
    interface Builder {
        fun build(): MainScreenComponent
    }

    fun inject(mainFeedActivity: MainFeedActivity)
}
