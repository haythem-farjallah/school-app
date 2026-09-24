# syntax=docker/dockerfile:1

# ---- Build ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
# The browser calls the API on the same origin; nginx proxies /api and /ws to the backend.
ENV VITE_API_URL=/api
RUN npm run build

# ---- Runtime ----
FROM nginx:1.28-alpine
# Backend location for the /api and /ws proxy, resolved when the container starts.
ENV BACKEND_URL=http://backend:8088
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
