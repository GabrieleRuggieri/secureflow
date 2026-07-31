"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { getUserManager, type User } from "@/lib/auth";

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  token: string | undefined;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const mgr = getUserManager();
    const current = await mgr.getUser();
    setUser(current && !current.expired ? current : null);
  }, []);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await refresh();
      } finally {
        if (active) setLoading(false);
      }
    })();
    const mgr = getUserManager();
    const onUserLoaded = (u: User) => setUser(u);
    const onUserUnloaded = () => setUser(null);
    mgr.events.addUserLoaded(onUserLoaded);
    mgr.events.addUserUnloaded(onUserUnloaded);
    return () => {
      active = false;
      mgr.events.removeUserLoaded(onUserLoaded);
      mgr.events.removeUserUnloaded(onUserUnloaded);
    };
  }, [refresh]);

  const login = useCallback(async () => {
    await getUserManager().signinRedirect();
  }, []);

  const logout = useCallback(async () => {
    await getUserManager().signoutRedirect();
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      token: user?.access_token,
      login,
      logout,
      refresh,
    }),
    [user, loading, login, logout, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
