package com.digitalid.infrastructure;

import com.digitalid.domain.DigitalID;
import com.digitalid.exception.IDNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of IdentityRepository using a HashMap.

 * The key is the idNumber (UUID string). The value is the DigitalID object.
 * Calling save() with an existing idNumber simply replaces the old object, which
 * means save() works for both creating new records and updating existing ones.

 * This implementation stores data in memory only. All data is lost when the
 * program stops.
 */
public class InMemoryIdentityRepository implements IdentityRepository {
    private final Map<String, DigitalID> store = new HashMap<>();

    /**
     * Saves a Digital ID into the HashMap. If a record with the same idNumber
     * already exists it is overwritten — this is intentional and allows updates.
     */
    @Override
    public void save(DigitalID digitalID) {
        store.put(digitalID.getIdNumber(), digitalID);
    }

    /** Looks up a Digital ID by its idNumber. Throws IDNotFoundException if it's not in the map. */
    @Override
    public DigitalID findById(String idNumber) throws IDNotFoundException {
        DigitalID found = store.get(idNumber);
        if (found == null) {
            throw new IDNotFoundException("Digital ID not found: " + idNumber);
        }
        return found;
    }

    /**
     * Returns true if a Digital ID with the given idNumber exists in the store.
     * Uses containsKey() on the HashMap.
     */
    @Override
    public boolean exists(String idNumber) {
        return store.containsKey(idNumber);
    }

    @Override
    public Collection<DigitalID> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }
}