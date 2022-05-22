/*
 *     Copyright 2019 - 2022 Tyler Williamson
 *
 *     This file is part of QuickWeather.
 *
 *     QuickWeather is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     QuickWeather is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.util;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.ominous.tylerutils.util.WindowUtils;

public class FullscreenHelper {
    private final Rect initialRect = new Rect();
    private final Rect initialMargins = new Rect();
    private final Rect fullscreenRect = new Rect();
    private final View currentView;
    private final ViewGroup currentFullscreenContainer;
    private final ValueAnimator animatorOpen;
    private final ValueAnimator animatorClose;
    private ViewGroup currentViewParent;
    private ViewGroup.LayoutParams currentInitialLayoutParams;
    private FrameLayout.LayoutParams fullscreenViewLayoutParams;

    private FullscreenState fullscreenState;

    public FullscreenHelper(Window window, View view, ViewGroup fullscreenContainer) {
        currentFullscreenContainer = fullscreenContainer;
        currentView = view;

        animatorOpen = ValueAnimator.ofFloat(1f, 0f);
        animatorOpen.addUpdateListener(valueAnimator -> doAnimation((Float) valueAnimator.getAnimatedValue()));
        animatorOpen.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                doAnimation(0f);

                WindowUtils.setImmersive(window, true);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                doAnimation(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        animatorClose = ValueAnimator.ofFloat(0f, 1f);
        animatorClose.addUpdateListener(valueAnimator -> doAnimation((Float) valueAnimator.getAnimatedValue()));
        animatorClose.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentFullscreenContainer.removeView(currentView);

                if (currentView.getParent() != null) {
                    ((ViewGroup) currentView.getParent()).removeView(currentView);
                }

                if (currentViewParent != null) {
                    currentViewParent.addView(currentView, currentInitialLayoutParams);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                WindowUtils.setImmersive(window, false);
                doAnimation(0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void fullscreenify(FullscreenState fullscreenState) {
        this.fullscreenState = fullscreenState;

        if (!animatorOpen.isRunning() && !animatorClose.isRunning()) {
            int duration = fullscreenState == FullscreenState.OPEN || fullscreenState == FullscreenState.CLOSED ? 0 : 250;

            boolean isFullscreen = isFullscreen();

            fullscreenViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            currentFullscreenContainer.getGlobalVisibleRect(fullscreenRect);

            if (isFullscreen) {
                if (currentView != null) {
                    currentView.getGlobalVisibleRect(initialRect);

                    initialMargins.set(initialRect.left, initialRect.top - fullscreenRect.top, fullscreenRect.right - initialRect.right, fullscreenRect.bottom - initialRect.bottom);

                    currentViewParent = (ViewGroup) currentView.getParent();
                    currentInitialLayoutParams = currentView.getLayoutParams();

                    if (currentViewParent != null) {
                        currentViewParent.removeView(currentView);
                    }

                    currentFullscreenContainer.addView(currentView);
                }

                animatorOpen.setDuration(duration);
                animatorOpen.start();
            } else {
                animatorClose.setDuration(duration);
                animatorClose.start();
            }
        }
    }

    private void doAnimation(float f) {
        if (currentView != null) {
            fullscreenViewLayoutParams.setMargins((int) (initialMargins.left * f), (int) (initialMargins.top * f), (int) (initialMargins.right * f), (int) (initialMargins.bottom * f));
            currentView.setLayoutParams(fullscreenViewLayoutParams);
        }
    }

    public boolean isFullscreen() {
        return fullscreenState == FullscreenState.OPEN || fullscreenState == FullscreenState.OPENING;
    }

    public enum FullscreenState {
        OPEN,
        OPENING,
        CLOSED,
        CLOSING
    }
}
