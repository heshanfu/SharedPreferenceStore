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

package uk.co.glass_software.android.shared_preferences;

import android.content.Context;

import io.reactivex.Observable;
import uk.co.glass_software.android.shared_preferences.keystore.KeyStoreModule;
import uk.co.glass_software.android.shared_preferences.persistence.PersistenceModule;
import uk.co.glass_software.android.shared_preferences.persistence.base.StoreEntry;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.SharedPreferenceStore;

public class StoreEntryFactory {
    
    private final SharedPreferenceStore store;
    private final SharedPreferenceStore encryptedStore;
    
    public StoreEntryFactory(Context context) {
        SharedPreferenceComponent component = DaggerSharedPreferenceComponent.builder()
                                                                             .keyStoreModule(new KeyStoreModule())
                                                                             .persistenceModule(new PersistenceModule(context.getApplicationContext()))
                                                                             .build();
        
        encryptedStore = component.encryptedStore();
        store = component.store();
        
        open("test", String.class);
    }
    
    public <C> StoreEntry<StoreEntry.UniqueKeyProvider, C> open(String key,
                                                                Class<C> valueClass) {
        return open(() -> key, valueClass);
    }
    
    public <C> StoreEntry<StoreEntry.UniqueKeyProvider, C> openEncrypted(String key,
                                                                         Class<C> valueClass) {
        return openEncrypted(() -> key, valueClass);
    }
    
    public <KEY extends StoreEntry.UniqueKeyProvider, C> StoreEntry<KEY, C> open(KEY key,
                                                                                 Class<C> valueClass) {
        return new StoreEntry<>(store, key, valueClass);
    }
    
    public <KEY extends StoreEntry.UniqueKeyProvider, C> StoreEntry<KEY, C> openEncrypted(KEY key,
                                                                                          Class<C> valueClass) {
        return new StoreEntry<>(encryptedStore, key, valueClass);
    }
    
    public Observable<String> observeChanges(){
        return store.observeChanges().mergeWith(encryptedStore.observeChanges());
    }
    
    public SharedPreferenceStore getStore() {
        return store;
    }
    
    public SharedPreferenceStore getEncryptedStore() {
        return encryptedStore;
    }
}
