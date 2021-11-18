package org.stepik.android.view.auth.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_auth_credential.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.analytic.LoginInteractionType
import org.stepic.droid.base.App
import org.stepic.droid.model.Credentials
import org.stepic.droid.ui.activities.SmartLockActivityBase
import org.stepic.droid.ui.dialogs.LoadingProgressDialogFragment
import org.stepic.droid.ui.util.setOnKeyboardOpenListener
import org.stepic.droid.util.ProgressHelper
import org.stepic.droid.util.toBundle
import org.stepik.android.domain.auth.model.LoginFailType
import org.stepik.android.model.Course
import org.stepik.android.presentation.auth.CredentialAuthPresenter
import org.stepik.android.presentation.auth.CredentialAuthView
import org.stepik.android.view.auth.extension.getMessageFor
import org.stepik.android.view.auth.model.AutoAuth
import org.stepik.android.view.base.ui.span.TypefaceSpanCompat
import ru.nobird.android.view.base.ui.extension.hideKeyboard
import javax.inject.Inject

class CredentialAuthActivity : SmartLockActivityBase(), CredentialAuthView {
    companion object {
        private const val EXTRA_EMAIL = "extra_email"
        private const val EXTRA_PASSWORD = "extra_password"
        private const val EXTRA_AUTO_AUTH = "extra_auto_auth"

        private const val EXTRA_COURSE = "extra_course"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        fun createIntent(
            context: Context,
            email: String? = null,
            password: String? = null,
            autoAuth: AutoAuth = AutoAuth.NONE,

            course: Course? = null
        ): Intent =
            Intent(context, CredentialAuthActivity::class.java)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_PASSWORD, password)
                .putExtra(EXTRA_AUTO_AUTH, autoAuth)
                .putExtra(EXTRA_COURSE, course)
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val credentialAuthPresenter: CredentialAuthPresenter by viewModels { viewModelFactory }

    private val progressDialogFragment: DialogFragment =
        LoadingProgressDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_credential)

        injectComponent()

        initTitle()

        forgotPasswordView.setOnClickListener {
            screenManager.openRemindPassword(this@CredentialAuthActivity)
        }

        loginField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                passwordField.requestFocus()
                handled = true
            }
            handled
        }

        passwordField.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                analytic.reportEvent(Analytic.Interaction.CLICK_SIGN_IN_NEXT_ON_SIGN_IN_SCREEN)
                analytic.reportEvent(Analytic.Login.REQUEST_LOGIN_WITH_INTERACTION_TYPE, LoginInteractionType.ime.toBundle())
                submit()
                handled = true
            }
            handled
        }

        val onFocusField = { _: View, hasFocus: Boolean ->
            if (hasFocus) {
                analytic.reportEvent(Analytic.Login.TAP_ON_FIELDS)
            }
        }
        loginField.setOnFocusChangeListener(onFocusField)
        passwordField.setOnFocusChangeListener(onFocusField)

        val reportAnalyticWhenTextBecomeNotBlank = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s.isNullOrBlank()) {
                    analytic.reportEvent(Analytic.Login.TYPING_TEXT_FIELDS)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                credentialAuthPresenter.onFormChanged()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        loginField.addTextChangedListener(reportAnalyticWhenTextBecomeNotBlank)
        passwordField.addTextChangedListener(reportAnalyticWhenTextBecomeNotBlank)

        launchSignUpButton.setOnClickListener {
            analytic.reportEvent(Analytic.Interaction.CLICK_SIGN_UP)
            screenManager
                .showRegistration(
                    this@CredentialAuthActivity,
                    intent.getParcelableExtra(EXTRA_COURSE)
                )
        }

        signInWithSocial.setOnClickListener { finish() }
        loginButton.setOnClickListener {
            analytic.reportEvent(Analytic.Interaction.CLICK_SIGN_IN_ON_SIGN_IN_SCREEN)
            analytic.reportEvent(Analytic.Login.REQUEST_LOGIN_WITH_INTERACTION_TYPE, LoginInteractionType.button.toBundle())
            submit()
        }
        loginRootView.requestFocus()

        initGoogleApiClient()

        setOnKeyboardOpenListener(root_view, {
            stepikLogo.isVisible = false
            signInText.isVisible = false
        }, {
            stepikLogo.isVisible = true
            signInText.isVisible = true
        })

        if (savedInstanceState == null) {
            setData(intent)
        }
    }

    private fun injectComponent() {
        App.component()
            .authComponentBuilder()
            .build()
            .inject(this)
    }

    override fun onStart() {
        super.onStart()
        credentialAuthPresenter.attachView(this)
    }

    override fun onStop() {
        credentialAuthPresenter.detachView(this)
        super.onStop()
    }

    private fun initTitle() {
        val signInString = getString(R.string.sign_in)
        val signInWithPasswordSuffix = getString(R.string.sign_in_with_password_suffix)

        val spannableSignIn = SpannableString(signInString + signInWithPasswordSuffix)
        val typeface = ResourcesCompat.getFont(this, R.font.roboto_medium)

        spannableSignIn.setSpan(TypefaceSpanCompat(typeface), 0, signInString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        signInText.text = spannableSignIn
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setData(intent)
    }

    private fun setData(intent: Intent) {
        val autoAuth = intent
            .getSerializableExtra(EXTRA_AUTO_AUTH) as? AutoAuth
            ?: AutoAuth.NONE

        val email = intent.getStringExtra(EXTRA_EMAIL)
        val password = intent.getStringExtra(EXTRA_PASSWORD)

        loginField.setText(email)
        passwordField.setText(password)

        when {
            email == null ->
                loginField.requestFocus()

            passwordField == null ->
                passwordField.requestFocus()
        }

        if (autoAuth != AutoAuth.NONE) {
            submit(autoAuth)
        }
    }

    private fun submit(autoAuth: AutoAuth = AutoAuth.NONE) {
        currentFocus?.hideKeyboard()

        val login = loginField.text.toString()
        val password = passwordField.text.toString()

        credentialAuthPresenter.submit(Credentials(login, password), isRegistration = autoAuth == AutoAuth.REGISTRATION)
    }

    override fun applyTransitionPrev() {} // we need default system animation

    override fun setState(state: CredentialAuthView.State) {
        if (state is CredentialAuthView.State.Loading) {
            ProgressHelper.activate(progressDialogFragment, supportFragmentManager, LoadingProgressDialogFragment.TAG)
        } else {
            ProgressHelper.dismiss(supportFragmentManager, LoadingProgressDialogFragment.TAG)
        }

        when (state) {
            is CredentialAuthView.State.Idle -> {
                loginButton.isEnabled = true
                loginForm.isEnabled = true
                loginErrorMessage.isVisible = false
            }

            is CredentialAuthView.State.Error -> {
                loginErrorMessage.text = getMessageFor(state.failType)
                loginErrorMessage.isVisible = true

                if (state.failType == LoginFailType.EMAIL_ALREADY_USED ||
                    state.failType == LoginFailType.EMAIL_PASSWORD_INVALID) {
                    loginForm.isEnabled = false
                    loginButton.isEnabled = false
                }
            }

            is CredentialAuthView.State.Success ->
                if (state.credentials != null && checkPlayServices() && googleApiClient?.isConnected == true) {
                    requestToSaveCredentials(state.credentials)
                } else {
                    openMainFeed()
                }
        }
    }

    override fun onCredentialsSaved() {
        openMainFeed()
    }

    private fun openMainFeed() {
        screenManager.showMainFeedAfterLogin(this, intent.getParcelableExtra(EXTRA_COURSE))
    }
}
