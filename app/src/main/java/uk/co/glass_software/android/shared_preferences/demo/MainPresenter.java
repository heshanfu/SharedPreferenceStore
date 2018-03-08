package uk.co.glass_software.android.shared_preferences.demo;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

import uk.co.glass_software.android.shared_preferences.SimpleLogger;
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory;
import uk.co.glass_software.android.shared_preferences.demo.model.Counter;
import uk.co.glass_software.android.shared_preferences.demo.model.LastOpenDate;
import uk.co.glass_software.android.shared_preferences.demo.model.Person;
import uk.co.glass_software.android.shared_preferences.demo.model.PersonEntry;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.EncryptedSharedPreferenceStore;
import uk.co.glass_software.android.shared_preferences.persistence.preferences.SharedPreferenceStore;

import static uk.co.glass_software.android.shared_preferences.persistence.PersistenceModule.ENCRYPTED_STORE_NAME;

class MainPresenter {
    
    private final SharedPreferences encryptedPreferences;
    private final SharedPreferenceStore store;
    private final EncryptedSharedPreferenceStore encryptedStore;
    private final StoreEntryFactory storeEntryFactory;
    private final Counter counter;
    private final LastOpenDate lastOpenDate;
    private final PersonEntry personEntry;
    
    MainPresenter(Context context) {
        encryptedPreferences = context.getSharedPreferences(
                context.getPackageName() + "$" + ENCRYPTED_STORE_NAME,
                Context.MODE_PRIVATE
        ); //used only to display encrypted values as stored on disk, should not be used directly in practice
        
        Gson gson = new Gson();
        storeEntryFactory = new StoreEntryFactory(
                context,
                new SimpleLogger(),
                new GsonSerialiser(gson)
        );
        store = storeEntryFactory.getStore();
        encryptedStore = storeEntryFactory.getEncryptedStore();
        
        counter = new Counter(store);
        lastOpenDate = new LastOpenDate(encryptedStore);
        personEntry = new PersonEntry(encryptedStore);
        createOrUpdatePerson();
    }
    
    private void createOrUpdatePerson() {
        Date lastSeenDate = new Date();
        Person person = personEntry.get();
        
        if (person == null) {
            person = new Person();
            person.setAge(30);
            person.setFirstName("John");
            person.setName("Smith");
        }
        
        person.setLastSeenDate(lastSeenDate);
        personEntry.save(person);
    }
    
    void onPause() {
        counter.save(counter.get(1) + 1);
        lastOpenDate.save(new Date());
        createOrUpdatePerson();
    }
    
    StoreEntryFactory storeEntryFactory() {
        return storeEntryFactory;
    }
    
    Counter counter() {
        return counter;
    }
    
    LastOpenDate lastOpenDate() {
        return lastOpenDate;
    }
    
    SharedPreferenceStore store() {
        return store;
    }
    
    SharedPreferenceStore encryptedStore() {
        return encryptedStore;
    }
    
    SharedPreferences encryptedPreferences() {
        return encryptedPreferences;
    }
    
    public String getKey(Map.Entry<String, ?> entry) {
        String decrypted = encryptedStore.decrypt(entry.getKey(), entry.getKey());
        return decrypted == null ? entry.getKey() : decrypted;
    }
}
