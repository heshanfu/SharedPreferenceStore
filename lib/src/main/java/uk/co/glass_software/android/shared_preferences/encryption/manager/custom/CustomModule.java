package uk.co.glass_software.android.shared_preferences.encryption.manager.custom;

import android.content.Context;
import android.support.annotation.Nullable;

import java.security.KeyStore;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import uk.co.glass_software.android.shared_preferences.Logger;
import uk.co.glass_software.android.shared_preferences.encryption.manager.key.KeyModule;
import uk.co.glass_software.android.shared_preferences.encryption.manager.key.KeyPairProvider;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.M;
import static uk.co.glass_software.android.shared_preferences.encryption.manager.key.KeyModule.KEY_ALIAS;

@Module(includes = KeyModule.class)
public class CustomModule {
    
    @Provides
    @Singleton
    PreMSecureKeyProvider providePreMSecureKeyProvider(KeyPairProvider keyPairProvider) {
        return new PreMSecureKeyProvider(keyPairProvider);
    }
    
    @Provides
    @Singleton
    PostMSecureKeyProvider providePostMSecureKeyProvider(@Nullable KeyStore keyStore,
                                                         @Named(KEY_ALIAS) String keyAlias) {
        return new PostMSecureKeyProvider(keyStore, keyAlias);
    }
    
    @Provides
    @Singleton
    @Nullable
    PreMEncryptionManager providePreMEncryptionManager(Logger logger,
                                                       PreMSecureKeyProvider secureKeyProvider,
                                                       @Named(KEY_ALIAS) String keyAlias,
                                                       @Nullable KeyStore keyStore,
                                                       Context applicationContext) {
        if (SDK_INT >= JELLY_BEAN_MR2
            && keyStore != null) {
            return new PreMEncryptionManager(
                    logger,
                    keyStore,
                    keyAlias,
                    secureKeyProvider,
                    applicationContext
            );
        }
        return null;
    }
    
    @Provides
    @Singleton
    @Nullable
    PostMEncryptionManager providePostMEncryptionManager(Logger logger,
                                                         PostMSecureKeyProvider secureKeyProvider,
                                                         @Named(KEY_ALIAS) String keyAlias,
                                                         @Nullable KeyStore keyStore) {
        if (SDK_INT >= M
            && keyStore != null) {
            return new PostMEncryptionManager(
                    logger,
                    secureKeyProvider,
                    keyStore,
                    keyAlias
            );
        }
        return null;
    }
}
