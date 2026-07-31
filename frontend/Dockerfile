# Multi-stage Next.js 16 frontend
# docker build -f frontend/Dockerfile -t secureflow-frontend:local .
FROM node:22-alpine AS deps
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY frontend/ .
ENV NEXT_TELEMETRY_DISABLED=1
# Public URLs via nginx (:80) — single origin for UI + IdP + API
ARG NEXT_PUBLIC_CORE_API_URL=http://localhost/core
ARG NEXT_PUBLIC_KEYCLOAK_URL=http://localhost/auth/realms/secureflow
ARG NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=secureflow-frontend
ARG NEXT_PUBLIC_OIDC_REDIRECT_URI=http://localhost/oidc/callback
ARG NEXT_PUBLIC_OIDC_POST_LOGOUT_URI=http://localhost/
ENV NEXT_PUBLIC_CORE_API_URL=$NEXT_PUBLIC_CORE_API_URL
ENV NEXT_PUBLIC_KEYCLOAK_URL=$NEXT_PUBLIC_KEYCLOAK_URL
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=$NEXT_PUBLIC_KEYCLOAK_CLIENT_ID
ENV NEXT_PUBLIC_OIDC_REDIRECT_URI=$NEXT_PUBLIC_OIDC_REDIRECT_URI
ENV NEXT_PUBLIC_OIDC_POST_LOGOUT_URI=$NEXT_PUBLIC_OIDC_POST_LOGOUT_URI
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
RUN addgroup -S secureflow && adduser -S secureflow -G secureflow
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
USER secureflow
EXPOSE 3000
CMD ["node", "server.js"]
