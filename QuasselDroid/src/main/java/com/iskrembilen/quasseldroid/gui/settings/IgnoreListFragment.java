/*
    QuasselDroid - Quassel client for Android
    Copyright (C) 2015 Ken Børge Viktil
    Copyright (C) 2015 Magnus Fjell
    Copyright (C) 2015 Martin Sandsmark <martin.sandsmark@kde.org>

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version, or under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License and the
    GNU Lesser General Public License along with this program.  If not, see
    <http://www.gnu.org/licenses/>.
 */

package com.iskrembilen.quasseldroid.gui.settings;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Function;
import com.iskrembilen.quasseldroid.R;
import com.iskrembilen.quasseldroid.protocol.state.Client;
import com.iskrembilen.quasseldroid.protocol.state.IdentityCollection;
import com.iskrembilen.quasseldroid.protocol.state.IgnoreListManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class IgnoreListFragment extends PreferenceFragment implements Observer {

    ListView list;
    SimpleListAdapter<IgnoreListManager.IgnoreListItem> adapter;
    private Formatter<IgnoreListManager.IgnoreListItem> formatter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_list, container, false);

        list = (ListView) root.findViewById(R.id.list);
        formatter = new Formatter<IgnoreListManager.IgnoreListItem>() {
            @Override
            public View format(IgnoreListManager.IgnoreListItem item, View holder, int position) {
                TextView textView = (TextView) holder.findViewById(R.id.text);
                Switch switchView = (Switch) holder.findViewById(R.id.action);

                textView.setText(item.getRule());
                switchView.setChecked(item.isActive());

                holder.setTag(position);

                return holder;
            }

            public void setListener(final View view, final OnItemInteractListener listener) {
                final Switch switchView = (Switch) view.findViewById(R.id.action);
                switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listener.onInteract(view, (int) view.getTag(), switchView.isChecked());
                    }
                });
            }
        };
        Client.getInstance().getIgnoreListManager().addObserver(this);

        initData();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * Callback method to be invoked when an item in this AdapterView has
             * been clicked.
             * <p/>
             * Implementers can call getItemAtPosition(position) if they need
             * to access the data associated with the selected item.
             *
             * @param parent   The AdapterView where the click happened.
             * @param view     The view within the AdapterView that was clicked (this
             *                 will be a view provided by the adapter)
             * @param position The position of the view in the adapter.
             * @param id       The row id of the item that was clicked.
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("IgnoreList", "click "+view.getTag().toString());
                Intent i = new Intent(getActivity(), IgnoreItemActivity.class);
                i.putExtra("ignoreId", (int) view.getTag());
                startActivity(i);
            }
        });

        return root;
    }

    private void initData() {
        adapter = new SimpleListAdapter<>(getActivity(), formatter, R.layout.widget_switchable_list_item);
        adapter.addAll(Client.getInstance().getIgnoreListManager().getIgnoreList());
        adapter.setOnItemInteractListener(new OnItemInteractListener() {
            @Override
            public void onInteract(View view, int position, Object state) {
                IgnoreListManager manager = Client.getInstance().getIgnoreListManager();
                IgnoreListManager.IgnoreListItem item = manager.getIgnoreList().get(position);
                item.setActive((boolean) state);
            }
        });
        list.setAdapter(adapter);
    }

    /**
     * This method is called if the specified {@code Observable} object's
     * {@code notifyObservers} method is called (because the {@code Observable}
     * object has been updated.
     *
     * @param observable the {@link java.util.Observable} object.
     * @param data       the data passed to {@link java.util.Observable#notifyObservers(Object)}.
     */
    @Override
    public void update(Observable observable, Object data) {
        if (getActivity()!=null)
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initData();
            }
        });
    }

    public static class SimpleListAdapter<T> extends ArrayAdapter<T> {

        private int mResource;
        private int mDropDownResource;
        private Context mContext;
        private LayoutInflater mInflater;
        private List<T> mObjects;
        private int mFieldId;
        private Formatter<T> mFormatter;
        private OnItemInteractListener listener;

        /**
         * Constructor
         *
         * @param context The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         */
        public SimpleListAdapter(Context context, Formatter<T> formatter, int resource) {
            super(context, resource, 0, new ArrayList<T>());
            init(context, formatter, resource, new ArrayList<T>());
        }

        /**
         * Constructor
         *
         * @param context The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects The objects to represent in the ListView.
         */
        public SimpleListAdapter(Context context, Formatter<T> formatter, int resource, T[] objects) {
            super(context, resource, 0, Arrays.asList(objects));
            init(context, formatter, resource, Arrays.asList(objects));
        }

        public void setOnItemInteractListener(OnItemInteractListener listener) {
            this.listener = listener;
        }

        /**
         * Constructor
         *
         * @param context The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects The objects to represent in the ListView.
         */
        public SimpleListAdapter(Context context, Formatter<T> formatter, int resource, List<T> objects) {
            super(context, resource, 0, objects);
            init(context, formatter, resource, objects);
        }

        private void init(Context context, Formatter<T> formatter, int resource, List<T> objects) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFormatter = formatter;
            mResource = resource;
            mObjects = objects;
        }

        /**
         * {@inheritDoc}
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, mResource);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent,
                                            int resource) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mInflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            T item = getItem(position);
            view = mFormatter.format(item, view, position);
            mFormatter.setListener(view, listener);
            return view;
        }
    }

    public interface Formatter<T> {
        public View format(T item, View holder, int position);
        public void setListener(View holder, OnItemInteractListener listener);
    }

    public interface OnItemInteractListener {
        public void onInteract(View view, int position, Object state);
    }
}

