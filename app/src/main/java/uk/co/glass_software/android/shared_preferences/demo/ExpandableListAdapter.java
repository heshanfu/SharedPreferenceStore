/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.shared_preferences.demo;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.SharedPreferenceStore;

class ExpandableListAdapter extends BaseExpandableListAdapter {
    
    private final LinkedList<String> headers;
    private final LinkedHashMap<String, List<String>> children;
    private final LayoutInflater inflater;
    private final MainPresenter presenter;
    private final SimpleDateFormat simpleDateFormat;
    
    ExpandableListAdapter(Context context,
                          MainPresenter presenter) {
        this.presenter = presenter;
        headers = new LinkedList<>();
        children = new LinkedHashMap<>();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
    }
    
    void showEntries() {
        headers.clear();
        children.clear();
        
        Date lastOpenDate = presenter.lastOpenDate().get();
        String formattedDate = lastOpenDate == null ? null : simpleDateFormat.format(lastOpenDate);
        
        addEntries("App opened",
                   "Count: " + presenter.counter().get(1) + " time(s)",
                   "Last open date: " + (formattedDate == null ? "N/A" : formattedDate)
        );
        
        addEntries("Plain text entries",
                   ((SharedPreferenceStore) presenter.store()).getCachedValues()
        );
        
        addEntries("Encrypted entries (as returned by the store)",
                   ((SharedPreferenceStore) presenter.encryptedStore()).getCachedValues()
        );
        
        addEntries("Encrypted entries (as stored on disk)",
                   presenter.encryptedPreferences().getAll()
        );
        
        notifyDataSetChanged();
    }
    
    private void addEntries(String header,
                            Map<String, ?> entries) {
        Observable.fromIterable(entries.entrySet())
                  .map(entry -> presenter.getKey(entry) + " => " + entry.getValue())
                  .toList()
                  .map(list -> list.toArray(new String[list.size()]))
                  .subscribe(list -> addEntries(header, list));
    }
    
    private void addEntries(String header,
                            String... subSections) {
        List<String> info = new ArrayList<>();
        headers.add(header);
        
        Observable.just(subSections)
                  .map(Arrays::asList)
                  .flatMap(Observable::fromIterable)
                  .map(string -> string.replaceAll("\\n", ""))
                  .toList()
                  .subscribe(list -> info.addAll(list));
        
        children.put(header, info);
    }
    
    @Override
    public Object getChild(int groupPosition,
                           int childPosition) {
        String key = headers.get(groupPosition);
        return children.get(key).get(childPosition);
    }
    
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    
    @Override
    public View getGroupView(int groupPosition,
                             boolean isExpanded,
                             View convertView,
                             ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_group, null);
        }
        
        TextView lblListHeader = convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);
        
        return convertView;
    }
    
    @Override
    public View getChildView(int groupPosition,
                             final int childPosition,
                             boolean isLastChild,
                             View convertView,
                             ViewGroup parent) {
        final String childText = (String) getChild(groupPosition, childPosition);
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
        }
        
        TextView txtListChild = convertView.findViewById(R.id.lblListItem);
        
        txtListChild.setText(childText);
        return convertView;
    }
    
    @Override
    public int getChildrenCount(int groupPosition) {
        return children.get(headers.get(groupPosition)).size();
    }
    
    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }
    
    @Override
    public int getGroupCount() {
        return headers.size();
    }
    
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }
    
    @Override
    public boolean hasStableIds() {
        return false;
    }
    
    @Override
    public boolean isChildSelectable(int groupPosition,
                                     int childPosition) {
        return false;
    }
}