# ==========================
# 1) Build Stage
# ==========================
FROM node:20-slim AS builder

WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json ./
# Install dependencies (we omit optional modules to avoid npm issues)
RUN npm install --omit=optional

# Copy the rest of the source code
COPY . .

# Build the app; force Rollup to use JS fallback by setting the env variable for this command
RUN ROLLUP_NO_NATIVE=true npm run build

# ==========================
# 2) Production Stage
# ==========================
FROM nginx:stable

# Clean the default nginx html directory
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Replace default nginx config with our custom config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 and start nginx
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
