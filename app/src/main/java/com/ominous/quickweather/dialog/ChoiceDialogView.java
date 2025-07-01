/*
 *   Copyright 2019 - 2025 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ominous.quickweather.R;

public class ChoiceDialogView<T> extends FrameLayout {
    private ChoiceAdapter<T> choiceAdapter;

    public ChoiceDialogView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public ChoiceDialogView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public ChoiceDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ChoiceDialogView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.dialog_choice, this, true);

        setAdapter(new ChoiceAdapter<>());
    }

    public void setAdapter(ChoiceAdapter<T> adapter) {
        choiceAdapter = adapter;
        RecyclerView recyclerView = findViewById(R.id.choice_recyclerview);

        if (recyclerView != null) {
            recyclerView.setAdapter(choiceAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    public void setItems(T[] items, String[] names) {
        choiceAdapter.setItems(items, names);
    }

    public void setSelected(T selected) {
        choiceAdapter.setSelected(selected);
    }

    public T getSelected() {
        return choiceAdapter.getSelected();
    }

    public static class ChoiceAdapter<T> extends RecyclerView.Adapter<ChoiceViewHolder> {
        private int selectedPosition = RecyclerView.NO_POSITION;
        private String[] names;
        protected T[] items;

        public ChoiceAdapter() {
        }

        public void setItems(T[] items, String[] names) {
            this.items = items;
            this.names = names;
        }

        @NonNull
        @Override
        public ChoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ChoiceViewHolder viewHolder = new ChoiceViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.item_choice_dialog, parent, false));



            viewHolder.itemView.setOnClickListener(v -> {
                setSelectedIndex(viewHolder.getLayoutPosition());
                v.setSelected(true);
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ChoiceViewHolder holder, int position) {
            TextView itemTextView = holder.itemView.findViewById(R.id.item_textview);

            itemTextView.setText(names[position]);

            holder.itemView.setSelected(selectedPosition == position);
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

        private void setSelectedIndex(int position) {
            int prevPosition = selectedPosition;
            selectedPosition = position;

            if (prevPosition != selectedPosition) {
                notifyItemChanged(prevPosition);
            }
        }

        public T getSelected() {
            return items[selectedPosition];
        }

        public void setSelected(T selected) {
            for (int i = 0, l = items.length; i < l; i++) {
                if ((items[i] == null && selected == null) ||
                        (items[i] != null && items[i].equals(selected))) {
                    selectedPosition = i;
                }
            }
        }
    }

    public static class ChoiceViewHolder extends RecyclerView.ViewHolder {
        public ChoiceViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}