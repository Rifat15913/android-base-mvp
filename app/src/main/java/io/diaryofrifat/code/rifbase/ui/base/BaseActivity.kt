package io.diaryofrifat.code.rifbase.ui.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import butterknife.ButterKnife
import io.diaryofrifat.code.utils.helper.ViewUtils
import timber.log.Timber

abstract class BaseActivity<V : MvpView, P : BasePresenter<V>>
    : AppCompatActivity(), MvpView, View.OnClickListener, View.OnFocusChangeListener {

    /**
     * LifecycleRegistry is an implementation of Lifecycle that can handle multiple observers.
     * It is used by Fragments and Support Library Activities.
     * You can also directly use it if you have a custom LifecycleOwner.
     */
    private val mLifecycleRegistry = LifecycleRegistry(this)

    /**
     * Fields
     * */
    // Child class has to pass it's layout resource id via this field
    protected abstract val layoutId: Int
    // Child class data binding object for views
    protected var viewDataBinding: ViewDataBinding? = null
        private set
    protected var menu: Menu? = null
        private set
    protected var currentFragment: BaseFragment<V, P>? = null
        private set
    protected var presenter: P? = null

    /**
     * The methods to be implemented by the child class (Abstract methods)
     * */
    // This method initializes the presenter
    protected abstract fun getActivityPresenter(): P

    // This method is called when activity initialization gets completed
    protected abstract fun startUI()

    // This method is called when activity gets destroyed
    protected abstract fun stopUI()

    /**
     * Optional to be overridden methods
     * */
    // Child class will pass the status bar color resource id by this method
    protected fun getStatusBarColorResId(): Int {
        return INVALID_ID
    }

    // Child class will pass the toolbar id by this method
    protected fun getToolbar(): Toolbar? {
        return null
    }

    // This method sets if the back icon should be shown or not at toolbar
    protected fun shouldShowBackIconAtToolbar(): Boolean {
        return true
    }

    // Child class will pass the menu id by this method
    protected fun getMenuId(): Int {
        return INVALID_ID
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (layoutId > INVALID_ID) {
            initializeLayout()
            initializePresenter()
            initializeToolbar()
            initializeStatusBar()
        }

        this.startUI()
    }

    /**
     * This method initializes activity presenter
     * */
    private fun initializePresenter() {
        val viewModel = ViewModelProviders.of(this)
                .get(BaseViewModel<V, P>().javaClass)
        var isPresenterCreated = false

        if (viewModel.getPresenter() == null) {
            viewModel.setPresenter(getActivityPresenter())
            isPresenterCreated = true
        }

        presenter = viewModel.getPresenter()
        presenter?.attachLifecycle(lifecycle)
        presenter?.attachView(this as V)

        if (isPresenterCreated) {
            presenter?.onPresenterCreated()
        }

        presenter?.activity = this
    }

    /**
     * This method initializes the layout to the activity
     * */
    private fun initializeLayout() {
        try {
            viewDataBinding = DataBindingUtil.setContentView(this, layoutId)
        } catch (e: Exception) {
            Timber.e(e)
        }

        if (viewDataBinding == null) {
            setContentView(layoutId)
            ButterKnife.bind(this)
        }
    }

    /**
     * This method initializes the toolbar
     * */
    private fun initializeToolbar() {
        if (getToolbar() != null && shouldShowBackIconAtToolbar()) {
            setSupportActionBar(getToolbar())
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
    }

    /**
     * This method initializes the status bar
     * */
    private fun initializeStatusBar() {
        val statusBarColorResId = getStatusBarColorResId()

        if (statusBarColorResId > INVALID_ID) {
            ViewUtils.setStatusBarColor(this, statusBarColorResId)
        }
    }

    override fun onClick(view: View) {

    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (getMenuId() > INVALID_ID) {
            menuInflater.inflate(getMenuId(), menu)
            this.menu = menu
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.stopUI()

        presenter?.detachLifecycle(lifecycle)
        presenter?.detachView()
    }

    /**
     * This method sets title of the toolbar
     *
     * @param title toolbar title
     * */
    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    /**
     * This method sets subtitle of the toolbar
     *
     * @param subtitle toolbar subtitle
     * */
    fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }

    /**
     * This method sets both title and subtitle of toolbar
     *
     * @param title    toolbar title
     * @param subtitle toolbar subtitle
     * */
    fun setToolbarText(title: String, subtitle: String) {
        setTitle(title)
        setSubtitle(subtitle)
    }

    /**
     * This method sets click listener to multiple views
     *
     * @param views multiple views
     * */
    protected fun setClickListener(vararg views: View) {
        for (view in views) {
            view.setOnClickListener(this)
        }
    }

    /**
     * This method sets animation to multiple views
     *
     * @param animationResourceId resource id of an animation
     * @param views multiple views
     * */
    protected fun setAnimation(animationResourceId: Int, vararg views: View) {
        val animation = AnimationUtils.loadAnimation(this, animationResourceId)

        for (view in views) {
            view.startAnimation(animation)
        }
    }

    /**
     * This method starts a fragment
     *
     * @param viewId       int value
     * @param baseFragment fragment object
     * */
    protected fun commitFragment(viewId: Int, baseFragment: BaseFragment<V, P>) {
        supportFragmentManager
                .beginTransaction()
                .replace(viewId, baseFragment, baseFragment.javaClass.name)
                .commit()

        currentFragment = baseFragment
    }

    companion object {
        private const val INVALID_ID = -1

        /**
         * This method runs current activity
         *
         * @param context UI context
         * @param intent intent for current activity
         * */
        fun runCurrentActivity(context: Context, intent: Intent) {
            context.startActivity(intent)
        }
    }
}
