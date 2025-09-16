# ==========================
# 1) Build Stage
# ==========================
FROM node:20-slim AS builder

WORKDIR /app

# Copy package files and install dependencies.
# We use --omit=optional to avoid other optional dependency issues,
# and then force-install the missing Rollup native module.
COPY package.json package-lock.json ./
RUN npm install --omit=optional && npm install @rollup/rollup-linux-x64-gnu

# Copy the rest of the source code.
COPY . .

# Build the application.
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