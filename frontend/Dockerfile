# ==========================
# 1) Build Stage
# ==========================
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package & lock files first for better caching
COPY package.json package-lock.json ./
RUN npm install

# Copy the rest of the files and build
COPY . .
RUN npm run build

# ==========================
# 2) Production Stage
# ==========================
FROM nginx:alpine

# Remove the default Nginx web directory
RUN rm -rf /usr/share/nginx/html/*

# Copy compiled build artifacts from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our custom Nginx config to override the default
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
