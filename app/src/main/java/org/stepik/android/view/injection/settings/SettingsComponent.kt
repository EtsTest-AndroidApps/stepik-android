package org.stepik.android.view.injection.settings

import dagger.Subcomponent
import org.stepik.android.view.injection.feedback.FeedbackDataModule
import org.stepik.android.view.settings.ui.dialog.NightModeSettingDialogFragment
import org.stepik.android.view.settings.ui.fragment.SettingsFragment

@Subcomponent(modules = [
    SettingsModule::class,
    FeedbackDataModule::class
])
interface SettingsComponent {
    @Subcomponent.Builder
    interface Builder {
        fun build(): SettingsComponent
    }

    fun inject(settingsFragment: SettingsFragment)
    fun inject(nightModeSettingDialogFragment: NightModeSettingDialogFragment)
}