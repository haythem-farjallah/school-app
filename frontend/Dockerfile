# ==========================
# 1) Build Stage - OPTIMIZED
# ==========================
FROM node:20-slim AS builder

WORKDIR /app

# Install system dependencies for native modules (CRITICAL for @rollup)
RUN apt-get update && apt-get install -y \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Copy package files first for better Docker layer caching
COPY package.json package-lock.json ./

# Use npm ci for faster, reliable, reproducible builds
# Install dependencies with optimizations
RUN npm ci --omit=optional --prefer-offline --no-audit --no-fund \
    && npm install @rollup/rollup-linux-x64-gnu \
    && npm cache clean --force

# Copy source code (this layer will be cached if dependencies don't change)
COPY . .

# Build with production optimizations
RUN npm run build

# ==========================
# 2) Production Stage
# ==========================
FROM nginx:stable

# Remove the default Nginx HTML directory content.
RUN rm -rf /usr/share/nginx/html/*

# Copy the built files from the build stage.
COPY --from=builder /app/dist /usr/share/nginx/html

# Use your custom Nginx configuration.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80.
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]