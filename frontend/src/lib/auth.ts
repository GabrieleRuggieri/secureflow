"use client";

import { User, UserManager, WebStorageStateStore } from "oidc-client-ts";

const authority =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost:8180/auth/realms/secureflow";
const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "secureflow-frontend";
const redirectUri =
  process.env.NEXT_PUBLIC_OIDC_REDIRECT_URI ?? "http://localhost:3000/auth/callback";
const postLogoutRedirectUri =
  process.env.NEXT_PUBLIC_OIDC_POST_LOGOUT_URI ?? "http://localhost:3000/";

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
