package com.marshalchen.ultimaterecyclerview.demo.scrollableobservable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.marshalchen.ultimaterecyclerview.ObservableScrollState;
import com.marshalchen.ultimaterecyclerview.ObservableScrollViewCallbacks;
import com.marshalchen.ultimaterecyclerview.UltimateRecycleObservableExtendedView;
import com.marshalchen.ultimaterecyclerview.Utils.ScrollUtils;
import com.marshalchen.ultimaterecyclerview.Utils.Scrollable;
import com.marshalchen.ultimaterecyclerview.demo.R;
import com.marshalchen.ultimaterecyclerview.demo.scrollableobservable.widget.SlidingTabLayout;
import com.marshalchen.ultimaterecyclerview.observables.CacheFragmentStatePagerAdapter;
import com.marshalchen.ultimaterecyclerview.observables.ObservableRecyclerView;
import com.marshalchen.ultimaterecyclerview.observables.TouchInterceptionLayout;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * Created by hesk on 12/6/15.
 */
public class ViewPagerTabFragmentParentFragment extends BaseFragment implements ObservableScrollViewCallbacks {

    public static final String FRAGMENT_TAG = "fragmentViewPager";

    private TouchInterceptionLayout mInterceptionLayout;
    private ViewPager mPager;
    private NavigationAdapter mPagerAdapter;
    private int mSlop;
    private boolean mScrolled;
    private ObservableScrollState mLastScrollState;
    private View mHeaderView, headerBanner;
    private int slidingTabLayout_height, mBaseTranslationY;
    private SlidingTabLayout slidingTabLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simplefragment_viewpaper_fragment_parent, container, false);

        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        mPagerAdapter = new NavigationAdapter(getChildFragmentManager());
        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mHeaderView = (View) view.findViewById(R.id.header);
        headerBanner = view.findViewById(R.id.header_background);

        slidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        slidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);
        slidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.accent));
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setViewPager(mPager);


        slidingTabLayout_height = slidingTabLayout.getHeight();
        ViewConfiguration vc = ViewConfiguration.get(parentActivity);
        mSlop = vc.getScaledTouchSlop();
        mInterceptionLayout = (TouchInterceptionLayout) view.findViewById(R.id.container);
        mInterceptionLayout.setScrollInterceptionListener(mInterceptionListener);
        mPager.setCurrentItem(0);

        return view;
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        if (dragging) {
            int toolbarHeight = headerBanner.getHeight();
            float currentHeaderTranslationY = ViewCompat.getTranslationY(mHeaderView);
            if (firstScroll) {
                if (-toolbarHeight < currentHeaderTranslationY) {
                    mBaseTranslationY = scrollY;
                }
            }
            float headerTranslationY = ScrollUtils.getFloat(-(scrollY - mBaseTranslationY), -toolbarHeight, 0);
            ViewPropertyAnimator.animate(mHeaderView).cancel();
            ViewCompat.setTranslationY(mHeaderView, headerTranslationY);
        }
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ObservableScrollState scrollState) {
        if (!mScrolled) {
            // This event can be used only when TouchInterceptionFrameLayout
            // doesn't handle the consecutive events.
            // adjustToolbar(scrollState);
            mBaseTranslationY = 0;

            Fragment fragment = getCurrentFragment();
            if (fragment == null) {
                return;
            }
            View view = fragment.getView();
            if (view == null) {
                return;
            }
            int toolbarHeight = headerBanner.getHeight();
            adjustToolbar(mLastScrollState, view);
        }
    }

    private TouchInterceptionLayout.TouchInterceptionListener mInterceptionListener = new TouchInterceptionLayout.TouchInterceptionListener() {
        @Override
        public boolean shouldInterceptTouchEvent(MotionEvent ev, boolean moving, float diffX, float diffY) {
            if (!mScrolled && mSlop < Math.abs(diffX) && Math.abs(diffY) < Math.abs(diffX)) {
                // Horizontal scroll is maybe handled by ViewPager
                return false;
            }

            Scrollable scrollable = getCurrentScrollable();
            if (scrollable == null) {
                mScrolled = false;
                return false;
            }

            // If interceptionLayout can move, it should intercept.
            // And once it begins to move, horizontal scroll shouldn't work any longer.
            View toolbarView = getActivity().findViewById(R.id.toolbar);
            int toolbarHeight = headerBanner.getHeight();


            int translationY = (int) ViewCompat.getTranslationY(mInterceptionLayout);
            boolean scrollingUp = 0 < diffY;
            boolean scrollingDown = diffY < 0;
            if (scrollingUp) {
                if (translationY < 0) {
                    mScrolled = true;
                    mLastScrollState = ObservableScrollState.UP;
                    return true;
                }
            } else if (scrollingDown) {
                if (-toolbarHeight < translationY) {
                    mScrolled = true;
                    mLastScrollState = ObservableScrollState.DOWN;
                    return true;
                }
            }
            mScrolled = false;
            return false;
        }

        @Override
        public void onDownMotionEvent(MotionEvent ev) {
        }

        @Override
        public void onMoveMotionEvent(MotionEvent ev, float diffX, float diffY) {
            View toolbarView = getActivity().findViewById(R.id.toolbar);
            float translationY = ScrollUtils.getFloat(ViewCompat.getTranslationY(mInterceptionLayout) + diffY, -toolbarView.getHeight(), 0);
            ViewCompat.setTranslationY(mInterceptionLayout, translationY);
            ViewCompat.setTranslationY(toolbarView, translationY);
            if (translationY < 0) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInterceptionLayout.getLayoutParams();
                lp.height = (int) (-translationY + getScreenHeight());
                mInterceptionLayout.requestLayout();
            }
        }

        @Override
        public void onUpOrCancelMotionEvent(MotionEvent ev) {
            mScrolled = false;
            adjustToolbar(mLastScrollState);
        }
    };

    private Scrollable getCurrentScrollable() {
        Fragment fragment = getCurrentFragment();
        if (fragment == null) {
            return null;
        }
        View view = fragment.getView();
        if (view == null) {
            return null;
        }

        UltimateRecycleObservableExtendedView exv = (UltimateRecycleObservableExtendedView) view.findViewById(R.id.scroll);
        return exv.getScrollable();
    }

    private void adjustToolbar(ObservableScrollState scrollState) {
        View toolbarView = getActivity().findViewById(R.id.toolbar);
        int toolbarHeight = toolbarView.getHeight();
        final Scrollable scrollable = getCurrentScrollable();
        if (scrollable == null) {
            return;
        }
        int scrollY = scrollable.getCurrentScrollY();
        if (scrollState == ObservableScrollState.DOWN) {
            showToolbar();
        } else if (scrollState == ObservableScrollState.UP) {
            if (toolbarHeight <= scrollY) {
                hideToolbar();
            } else {
                showToolbar();
            }
        } else if (!toolbarIsShown() && !toolbarIsHidden()) {
            // Toolbar is moving but doesn't know which to move:
            // you can change this to hideToolbar()
            showToolbar();
        }
    }


    private void adjustToolbar(ObservableScrollState scrollState, View view) {
        int toolbarHeight = headerBanner.getHeight();
        final Scrollable scrollView = (Scrollable) view.findViewById(R.id.scroll);
        if (scrollView == null) {
            return;
        }
        int scrollY = scrollView.getCurrentScrollY();
        if (scrollState == ObservableScrollState.DOWN) {
            showToolbar();
        } else if (scrollState == ObservableScrollState.UP) {
            if (toolbarHeight <= scrollY) {
                hideToolbar();
            } else {
                showToolbar();
            }
        } else {
            // Even if onScrollChanged occurs without scrollY changing, toolbar should be adjusted
            if (toolbarIsShown() || toolbarIsHidden()) {
                // Toolbar is completely moved, so just keep its state
                // and propagate it to other pages
                propagateToolbarState(toolbarIsShown());
            } else {
                // Toolbar is moving but doesn't know which to move:
                // you can change this to hideToolbar()
                showToolbar();
            }
        }
    }

    private void propagateToolbarState(boolean isShown) {
        final int toolbarHeight = headerBanner.getHeight();
        // Set scrollY for the fragments that are not created yet
        mPagerAdapter.setScrollY(isShown ? 0 : toolbarHeight);

        // Set scrollY for the active fragments
        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            // Skip current item
            if (i == mPager.getCurrentItem()) {
                continue;
            }
            // Skip destroyed or not created item
            Fragment f = mPagerAdapter.getItemAt(i);
            if (f == null) {
                continue;
            }
            View view = f.getView();
            if (view == null) {
                continue;
            }

            /*if (view.findViewById(R.id.scroll) instanceof ObservableListView) {
                ObservableListView listView = (ObservableListView) view.findViewById(R.id.scroll);
                if (isShown) {
                    // Scroll up
                    if (0 < listView.getCurrentScrollY()) {
                        listView.setSelection(0);
                    }
                } else {
                    // Scroll down (to hide padding)
                    if (listView.getCurrentScrollY() < toolbarHeight) {
                        listView.setSelection(1);
                    }
                }
            }*/
            if (view.findViewById(R.id.scroll) instanceof ObservableRecyclerView) {
                ObservableRecyclerView listView = (ObservableRecyclerView) view.findViewById(R.id.scroll);
                if (isShown) {
                    // Scroll up
                    if (0 < listView.getCurrentScrollY()) {
                        // listView.setSelection(0);
                        Log.d(FRAGMENT_TAG, "up");
                    }
                } else {
                    // Scroll down (to hide padding)
                    if (listView.getCurrentScrollY() < toolbarHeight) {
                        //listView.setSelection(1);
                        Log.d(FRAGMENT_TAG, "down");
                    }
                }
            }

        }
    }

    private Fragment getCurrentFragment() {
        return mPagerAdapter.getItemAt(mPager.getCurrentItem());
    }

    private boolean toolbarIsShown() {
        return ViewCompat.getTranslationY(mInterceptionLayout) == 0;
    }

    private boolean toolbarIsHidden() {
        View view = getView();
        if (view == null) {
            return false;
        }
        View toolbarView = getActivity().findViewById(R.id.toolbar);
        return ViewCompat.getTranslationY(mInterceptionLayout) == -toolbarView.getHeight();
    }

    private void showToolbar() {
        animateToolbar(0);
    }

    private void hideToolbar() {
        View toolbarView = getActivity().findViewById(R.id.toolbar);
        animateToolbar(-toolbarView.getHeight());
    }

    private void animateToolbar(final float toY) {
        float layoutTranslationY = ViewCompat.getTranslationY(mInterceptionLayout);
        if (layoutTranslationY != toY) {
            ValueAnimator animator = ValueAnimator.ofFloat(ViewCompat.getTranslationY(mInterceptionLayout), toY).setDuration(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float translationY = (float) animation.getAnimatedValue();
                    View toolbarView = getActivity().findViewById(R.id.toolbar);
                    ViewCompat.setTranslationY(mInterceptionLayout, translationY);
                    ViewCompat.setTranslationY(toolbarView, translationY);
                    if (translationY < 0) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInterceptionLayout.getLayoutParams();
                        lp.height = (int) (-translationY + getScreenHeight());
                        mInterceptionLayout.requestLayout();
                    }
                }
            });
            animator.start();
        }
    }

    /**
     * This adapter provides two types of fragments as an example.
     * {@linkplain #createItem(int)} should be modified if you use this example for your app.
     */
    private static class NavigationAdapter extends CacheFragmentStatePagerAdapter {

        private static final String[] TITLES = new String[]{"Applepie", "Butter Cookie", "Cupcake", "Donut", "Eclair", "Froyo", "Gingerbread", "Honeycomb", "Ice Cream Sandwich", "Jelly Bean", "KitKat", "Lollipop"};

        public NavigationAdapter(FragmentManager fm) {
            super(fm);
        }

        private int mScrollY;

        public void setScrollY(int scrollY) {
            mScrollY = scrollY;
        }

        @Override
        protected Fragment createItem(int position) {

            Fragment f = new ViewPagerFragmentListSingle();
            if (0 < mScrollY) {
                Bundle args = new Bundle();
                // args.putInt(ViewPagerTab2RecyclerViewFragment.ARG_INITIAL_POSITION, 1);
                f.setArguments(args);
            }

            return f;
            /*
            Fragment f;
            final int pattern = position % 5;
            switch (pattern) {
                case 0:
                    f = new ViewPagerTab2RecyclerViewFragment();
                    break;
                case 1:
                    f = new ViewPagerTab2RecyclerViewFragment();
                    break;
                case 2:
                    f = new ViewPagerTab2RecyclerViewFragment();
                    break;
                case 3:
                    f = new ViewPagerTab2RecyclerViewFragment();
                    break;
                case 4:
                default:
                    f = new ViewPagerTab2RecyclerViewFragment();
                    break;
            }
            return f;*/
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }
    }
}
