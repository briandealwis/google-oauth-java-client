/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.extensions.jdo.auth.oauth2;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.util.Preconditions;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Thread-safe JDO implementation of a credential store.
 *
 * @since 1.7
 * @author Yaniv Inbar
 */
public class JdoCredentialStore implements CredentialStore {

  /** Persistence manager factory. */
  private final PersistenceManagerFactory persistenceManagerFactory;

  /** Lock on storing, loading and deleting a credential. */
  private final Lock lock = new ReentrantLock();

  /**
   * @param persistenceManagerFactory persistence manager factory
   */
  public JdoCredentialStore(PersistenceManagerFactory persistenceManagerFactory) {
    this.persistenceManagerFactory = Preconditions.checkNotNull(persistenceManagerFactory);
  }

  public void store(String userId, Credential credential) {
    PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
    lock.lock();
    try {
      try {
        // update the current object by the given credential
        JdoPersistedCredential persistedCredential =
            persistenceManager.getObjectById(JdoPersistedCredential.class, userId);
        persistedCredential.update(credential);
      } catch (JDOObjectNotFoundException e) {
        // object doesn't exists, so persist a new object
        JdoPersistedCredential persistedCredential = new JdoPersistedCredential(userId, credential);
        persistenceManager.makePersistent(persistedCredential);
      }
    } finally {
      lock.unlock();
      persistenceManager.close();
    }
  }

  public void delete(String userId, Credential credential) {
    PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
    lock.lock();
    try {
      JdoPersistedCredential persistedCredential =
          persistenceManager.getObjectById(JdoPersistedCredential.class, userId);
      persistenceManager.deletePersistent(persistedCredential);
    } finally {
      lock.unlock();
      persistenceManager.close();
    }
  }

  public boolean load(String userId, Credential credential) {
    PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
    lock.lock();
    try {
      JdoPersistedCredential persistedCredential =
          persistenceManager.getObjectById(JdoPersistedCredential.class, userId);
      persistedCredential.load(credential);
      return true;
    } catch (JDOObjectNotFoundException e) {
      return false;
    } finally {
      lock.unlock();
      persistenceManager.close();
    }
  }
}
