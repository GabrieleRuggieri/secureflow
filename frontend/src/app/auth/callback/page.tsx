"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getUserManager } from "@/lib/auth";

export default function AuthCallbackPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        await getUserManager().signinRedirectCallback();
        router.replace("/dashboard");
      } catch (err) {
        setError(err instanceof Error ? err.message : "Auth callback failed");
      }
    })();
  }, [router]);

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 text-danger">
        {error}
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center text-muted">
      Completing sign-in…
    </div>
  );
}
