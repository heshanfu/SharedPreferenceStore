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

package uk.co.glass_software.android.shared_preferences.keystore;

import android.annotation.TargetApi;
import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import uk.co.glass_software.android.shared_preferences.Logger;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static uk.co.glass_software.android.shared_preferences.keystore.KeyStoreModule.ANDROID_KEY_STORE;

//see https://medium.com/@ericfu/securely-storing-secrets-in-an-android-application-501f030ae5a3#.qcgaaeaso
public class PreMKeyStoreManager extends BaseKeyStoreManager {
    
    private static final String ASYMMETRIC_ENCRYPTION = "RSA";
    private static final String ENCRYPTION = "AES";
    private static final String ENCRYPTION_PROVIDER = "BC";
    private static final String AES_MODE = "AES/ECB/PKCS7Padding";
    
    private final KeyStore keyStore;
    private final SavedEncryptedAesKey encryptedAesKey;
    private final String alias;
    private final Context applicationContext;
    
    public PreMKeyStoreManager(Logger logger,
                               KeyStore keyStore,
                               @Nullable SavedEncryptedAesKey encryptedAesKey,
                               String alias,
                               Context applicationContext) {
        super(logger);
        this.keyStore = keyStore;
        this.encryptedAesKey = encryptedAesKey;
        this.alias = alias;
        this.applicationContext = applicationContext;
    }
    
    @NonNull
    @Override
    protected Cipher getCipher(boolean isEncrypt) throws Exception {
        Key secretKey = getSecretKey();
        if (secretKey == null) {
            throw new IllegalStateException("Could not retrieve the secret key");
        }
        else {
            Cipher cipher = Cipher.getInstance(AES_MODE, ENCRYPTION_PROVIDER);
            cipher.init(isEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                        secretKey
            );
            return cipher;
        }
    }
    
    @Nullable
    private Key getSecretKey() throws Exception {
        if (encryptedAesKey == null) {
            throw new NullPointerException("Could not load encrypted AES key");
        }
        
        byte[] storedKey = encryptedAesKey.getBytes();
        
        if (storedKey == null) {
            return null;
        }
        
        return new SecretKeySpec(storedKey, ENCRYPTION);
    }
    
    @Override
    @TargetApi(JELLY_BEAN_MR2)
    protected synchronized void createNewKeyPairIfNeeded() {
        try {
            if (keyStore != null && !keyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);
                
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(applicationContext)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=" + alias))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        ASYMMETRIC_ENCRYPTION,
                        ANDROID_KEY_STORE
                );
                
                keyPairGenerator.initialize(spec);
                keyPairGenerator.generateKeyPair();
            }
        }
        catch (Exception e) {
            logger.e(this, e, "Could not create a new key");
        }
    }
}