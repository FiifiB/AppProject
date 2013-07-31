package com.aayfi.whrtigo.AuthorizationAndStore;

import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
/**
 * Interface that allows reading of authorisation token
 * @author Fiifi
 *
 */
public interface CredentialStore {

  AccessTokenResponse read();
  void write(AccessTokenResponse response);
  void clearCredentials();
}
