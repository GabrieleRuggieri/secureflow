"use client";

import { User, UserManager, WebStorageStateStore } from "oidc-client-ts";

/** Public origin is nginx http://localhost (UI + /auth + /core). */
const authority =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost/auth/realms/secureflow";
const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "secureflow-frontend";
const redirectUri =
  process.env.NEXT_PUBLIC_OIDC_REDIRECT_URI ?? "http://localhost/oidc/callback";
const postLogoutRedirectUri =
  process.env.NEXT_PUBLIC_OIDC_POST_LOGOUT_URI ?? "http://localhost/";

let manager: UserManager | null = null;

export function getUserManager(): UserManager {
  if (typeof window === "undefined") {
    throw new Error("UserManager is browser-only");
  }
  if (!manager) {
    manager = new UserManager({
      authority,
      client_id: clientId,
      redirect_uri: redirectUri,
      post_logout_redirect_uri: postLogoutRedirectUri,
      response_type: "code",
      scope: "openid profile email",
      automaticSilentRenew: true,
      userStore: new WebStorageStateStore({ store: window.localStorage }),
    });
  }
  return manager;
}

export type { User };
