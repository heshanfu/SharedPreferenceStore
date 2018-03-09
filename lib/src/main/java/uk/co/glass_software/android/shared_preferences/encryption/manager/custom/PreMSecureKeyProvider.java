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

package uk.co.glass_software.android.shared_preferences.encryption.manager.custom;

import android.annotation.TargetApi;
import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.Nullable;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import uk.co.glass_software.android.shared_preferences.Logger;
import uk.co.glass_software.android.shared_preferences.encryption.manager.key.RsaEncryptedKeyPairProvider;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static uk.co.glass_software.android.shared_preferences.encryption.manager.key.KeyModule.ANDROID_KEY_STORE;

public class PreMSecureKeyProvider implements SecureKeyProvider {
    
    private static final String KEY_ALGORITHM_RSA = "RSA";
    private static final String KEY_ALGORITHM_AES = "AES";
    private final RsaEncryptedKeyPairProvider keyPairProvider;
    private final Context applicationContext;
    private final String keyAlias;
    private final Logger logger;
    
    @Nullable
    private final KeyStore keyStore;
    
    PreMSecureKeyProvider(RsaEncryptedKeyPairProvider keyPairProvider,
                          Context applicationContext,
                          Logger logger,
                          @Nullable KeyStore keyStore,
                          String keyAlias) {
        this.keyPairProvider = keyPairProvider;
        this.applicationContext = applicationContext;
        this.logger = logger;
        this.keyStore = keyStore;
        this.keyAlias = keyAlias + "-preM";
        createNewKeyPairIfNeeded();
        keyPairProvider.initialise();
    }
    
    @Override
    public Key getKey() throws Exception {
        return new SecretKeySpec(keyPairProvider.getCipherKey(), KEY_ALGORITHM_AES);
    }
    
    @Override
    @TargetApi(JELLY_BEAN_MR2)
    public synchronized void createNewKeyPairIfNeeded() {
        try {
            if (keyStore != null && !keyStore.containsAlias(keyAlias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);
                
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(applicationContext)
                        .setAlias(keyAlias)
                        .setSubject(new X500Principal("CN=" + keyAlias))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        KEY_ALGORITHM_RSA,
                        ANDROID_KEY_STORE
                );
                
                keyPairGenerator.initialize(spec);
                keyPairGenerator.generateKeyPair();
                
                if(!keyStore.containsAlias(keyAlias)){
                    throw new IllegalStateException("Key pair was not generated");
                }
            }
        }
        catch (Exception e) {
            logger.e(this, e, "Could not create a new key");
        }
    }
    
    @Override
    public boolean isEncryptionSupported() {
        return keyStore != null;
    }
    
    @Override
    public boolean isEncryptionKeySecure() {
        return isEncryptionSupported();
    }
    
}
