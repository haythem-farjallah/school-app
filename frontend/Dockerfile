# ==========================
# 1) Build Stage
# ==========================
FROM node:20-slim AS builder

WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json ./
ENV ROLLUP_NO_NATIVE=true
RUN npm install --omit=optional

# Copy source and build
COPY . .
RUN npm run build

# ==========================
# 2) Production Stage
# ==========================
FROM nginx:stable

# Clean Nginx root and copy compiled files
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist /usr/share/nginx/html

# Use your own Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
