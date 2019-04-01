package com.ominous.quickweather.activity;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.ColorUtils;
import com.ominous.quickweather.util.ViewUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

abstract class OnboardingActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {

    //TODO fix when canAdvance(true) changes to canAdvance(false)

    private ViewPager viewPager;
    private ImageButton nextButton;
    private TextView finishButton;
    private LinearLayout indicators;
    private OnboardingPagerAdapter onboardingAdapter;

    private List<FragmentContainer> fragmentContainers = new ArrayList<>();

    protected abstract void addFragments();

    void addFragment(OnboardingFragment fragment) {
        fragmentContainers.add(new FragmentContainer(fragment));
    }

    private class FragmentContainer {
        ImageView indicator;
        OnboardingFragment fragment;

        FragmentContainer(OnboardingFragment fragment) {
            this.fragment = fragment;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= 26) {
            if (ColorUtils.getTextColor(getColor(R.color.background_primary)) == ColorUtils.COLOR_TEXT_BLACK) {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~ View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR & ~ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        this.setContentView(R.layout.activity_onboarding);

        nextButton = findViewById(R.id.button_next);
        finishButton = findViewById(R.id.button_finish);
        viewPager = findViewById(R.id.container);
        indicators = findViewById(R.id.indicators);

        this.addFragments();
        this.createIndicators();
        this.updateIndicators(0);

        onboardingAdapter = new OnboardingPagerAdapter(getSupportFragmentManager(), fragmentContainers);

        viewPager.setAdapter(onboardingAdapter);
        viewPager.addOnPageChangeListener(this);
        viewPager.setPageMargin((int) getResources().getDimension(R.dimen.margin_standard));

        findViewById(R.id.button_next).setOnClickListener(this);
        findViewById(R.id.button_finish).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_next:
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                break;
            case R.id.button_finish:
                if (onboardingAdapter.fragmentContainers.get(viewPager.getCurrentItem()).fragment.canAdvanceToNextFragment()) {
                    for (FragmentContainer fragmentContainer : fragmentContainers) {
                        fragmentContainer.fragment.onFinish();
                    }

                    this.onFinish();
                    this.finish();
                }

                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        updateIndicators(position);

        nextButton.setVisibility(position != fragmentContainers.size() - 1 && fragmentContainers.get(position).fragment.canAdvanceToNextFragment()  ? View.VISIBLE : View.GONE);
        finishButton.setVisibility(position == (fragmentContainers.size() - 1) && fragmentContainers.get(position).fragment.canAdvanceToNextFragment() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private void createIndicators() {
        int marginHalf = (int) getResources().getDimension(R.dimen.margin_half);
        FragmentContainer fragmentContainer;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(marginHalf, marginHalf);
        layoutParams.setMarginEnd(marginHalf);

        for (int i = 0, l = fragmentContainers.size(); i < l; i++) {
            fragmentContainer = fragmentContainers.get(i);
            fragmentContainer.indicator = new ImageView(this);

            fragmentContainer.indicator.setBackgroundResource(R.drawable.indicator_selected);

            indicators.addView(fragmentContainer.indicator, layoutParams);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0, l = fragmentContainers.size(); i < l; i++) {
            fragmentContainers.get(i).indicator.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(i == position ? R.color.text_primary_emphasis : R.color.text_primary_disabled)));
        }
    }

    abstract void onFinish();

    private void notifyViewPager() {
        onboardingAdapter.notifyDataSetChanged();

        onPageSelected(viewPager.getCurrentItem());
    }

    private class OnboardingPagerAdapter extends PagerAdapter {
        private List<FragmentContainer> fragmentContainers;
        private FragmentManager fragmentManager;

        OnboardingPagerAdapter(FragmentManager fragmentManager, List<FragmentContainer> fragmentContainers) {
            this.fragmentManager = fragmentManager;

            this.fragmentContainers = fragmentContainers;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,@NonNull Object object) {
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.remove(fragmentContainers.get(position).fragment);
            trans.commit();
            //fragmentContainers.get(position).fragment = null;
        }

        @NonNull
        @Override
        public Fragment instantiateItem(@NonNull ViewGroup container, int position) {
            //TODO This still means there are two Fragments out there. There must be a better way
            String tag = "fragment:" + position;

            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            Fragment.SavedState savedInstanceState = null;

            if (fragment != null) {
                FragmentTransaction trans = fragmentManager.beginTransaction();

                savedInstanceState = fragmentManager.saveFragmentInstanceState(fragment);

                trans.remove(fragment);
                trans.commit();
            }

            FragmentTransaction trans = fragmentManager.beginTransaction();

            fragment = fragmentContainers.get(position).fragment;
            fragment.setInitialSavedState(savedInstanceState);

            trans.add(container.getId(), fragment, tag);
            trans.commit();

            return fragment;
        }

        @Override
        public int getCount() {
            int count = 1;

            for (int i = 0, l = fragmentContainers.size(); i < l; i++) {
                if (fragmentContainers.get(i).fragment.canAdvanceToNextFragment()) {
                    count++;
                }
            }

            //for (int i=count,l=fragmentContainers.size();i<l;i++) {
            //    destroyItem(viewPager,i,fragmentContainers.get(i).fragment);
            //}

            return Math.min(count, fragmentContainers.size());
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return ((Fragment) o).getView() == view;
        }

    }

    public static abstract class OnboardingFragment extends Fragment {
        private boolean canAdvance = false;
        WeakReference<FragmentActivity> activity;

        public void notifyViewPager(boolean canAdvance) {
            this.canAdvance = canAdvance;

            if (this.getActivity() != null) {
                ((OnboardingActivity) this.getActivity()).notifyViewPager();
            }
        }

        private boolean canAdvanceToNextFragment() {
            return canAdvance;
        }

        abstract void onFinish();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            //this.setRetainInstance(true);
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

            this.activity = new WeakReference<>(getActivity());
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }
}
