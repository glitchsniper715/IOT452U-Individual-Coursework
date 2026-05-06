package com.digitalid.infrastructure;

import com.digitalid.domain.DigitalID;
import com.digitalid.exception.IDNotFoundException;

import java.util.Collection;

/**
 * Defines the contract for storing and retrieving Digital ID objects.
 * IdentityManager will depend on this interface rather than on InMemoryIdentityRepository directly.
 */
public interface IdentityRepository {

    /**
     * Saves a Digital ID to the store. If a Digital ID with the same idNumber
     * already exists, it is replaced (used for updates as well as creates).
     */
    void save(DigitalID digitalID);

    /** Retrieves a Digital ID by its unique ID number.*/
    DigitalID findById(String idNumber) throws IDNotFoundException;

    /** Checks whether a Digital ID with the given ID number exists in the store. */
    boolean exists(String idNumber);

    /** Returns all Digital IDs currently in the store. */
    Collection<DigitalID> findAll();
}