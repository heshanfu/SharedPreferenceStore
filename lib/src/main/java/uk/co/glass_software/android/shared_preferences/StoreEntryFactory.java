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
import android.support.annotation.Nullable;

import io.reactivex.Observable;
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager;
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManagerModule;
import uk.co.glass_software.android.shared_preferences.encryption.manager.key.KeyModule;
import uk.co.glass_software.android.shared_preferences.persistence.base.KeyValueStore;
import uk.co.glass_software.android.shared_preferences.persistence.base.StoreEntry;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.EncryptedSharedPreferenceStore;
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.SharedPreferenceStore;

public class StoreEntryFactory {
    
    private final SharedPreferenceStore store;
    private final EncryptedSharedPreferenceStore encryptedStore;
    private final KeyValueStore lenientEncryptedStore;
    private final boolean isEncryptionSupported;
    private final boolean isEncryptionKeySecure;
    
    @Nullable
    private final EncryptionManager encryptionManager;
    
    public StoreEntryFactory(Context context) {
        this(context, null);
    }
    
    public StoreEntryFactory(Context context,
                             @Nullable Serialiser customSerialiser) {
        this(context, null, customSerialiser);
    }
    
    public StoreEntryFactory(Context context,
                             @Nullable Logger logger,
                             @Nullable Serialiser customSerialiser) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferenceComponent component = DaggerSharedPreferenceComponent
                .builder()
                .keyModule(new KeyModule(context.getApplicationContext()))
                .encryptionManagerModule(new EncryptionManagerModule(true))
                .persistenceModule(new PersistenceModule(applicationContext, logger, customSerialiser))
                .build();
        
        encryptedStore = component.encryptedStore();
        store = component.store();
        lenientEncryptedStore = component.lenientEncryptedStore();
        encryptionManager = component.keyStoreManager();
        isEncryptionSupported = component.isEncryptionSupported();
        isEncryptionKeySecure = component.isEncryptionKeySecure();
        component.logger().d(this, "Encryption supported: " + (isEncryptionSupported ? "TRUE" : "FALSE"));
        component.logger().d(this, "Encryption key secure: " + (isEncryptionKeySecure ? "TRUE" : "FALSE"));
    }
    
    public <C> StoreEntry<C> open(String key,
                                  Class<C> valueClass) {
        return open(() -> key, () -> valueClass);
    }
    
    public <C> StoreEntry<C> openEncrypted(String key,
                                           Class<C> valueClass) {
        return openEncrypted(() -> key, () -> valueClass);
    }
    
    public <C> StoreEntry<C> openLenientEncrypted(String key,
                                                  Class<C> valueClass) {
        return openLenientEncrypted(() -> key, () -> valueClass);
    }
    
    public <C, K extends StoreEntry.UniqueKeyProvider & StoreEntry.ValueClassProvider> StoreEntry<C> open(K key) {
        return open(key, key);
    }
    
    public <C, K extends StoreEntry.UniqueKeyProvider & StoreEntry.ValueClassProvider> StoreEntry<C> openEncrypted(K key) {
        return openEncrypted(key, key);
    }
    
    public <C, K extends StoreEntry.UniqueKeyProvider & StoreEntry.ValueClassProvider> StoreEntry<C> openLenientEncrypted(K key) {
        return openEncrypted(key, key);
    }
    
    private <C> StoreEntry<C> open(StoreEntry.UniqueKeyProvider keyProvider,
                                   StoreEntry.ValueClassProvider valueClassProvider) {
        return new StoreEntry<>(store, keyProvider, valueClassProvider);
    }
    
    private <C> StoreEntry<C> openEncrypted(StoreEntry.UniqueKeyProvider keyProvider,
                                            StoreEntry.ValueClassProvider valueClassProvider) {
        return new StoreEntry<>(encryptedStore, keyProvider, valueClassProvider);
    }
    
    private <C> StoreEntry<C> openLenientEncrypted(StoreEntry.UniqueKeyProvider keyProvider,
                                                   StoreEntry.ValueClassProvider valueClassProvider) {
        return new StoreEntry<>(encryptedStore, keyProvider, valueClassProvider);
    }
    
    public boolean isEncryptionSupported() {
        return isEncryptionSupported;
    }
    
    public boolean isEncryptionKeySecure() {
        return isEncryptionKeySecure;
    }
    
    public Observable<String> observeChanges() {
        return store.observeChanges().mergeWith(encryptedStore.observeChanges());
    }
    
    public SharedPreferenceStore getStore() {
        return store;
    }
    
    public EncryptedSharedPreferenceStore getEncryptedStore() {
        return encryptedStore;
    }
    
}
