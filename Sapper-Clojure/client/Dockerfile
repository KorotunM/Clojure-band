# Stage 1: build React app
FROM node:20.13.1-bookworm-slim AS builder

WORKDIR /app

# Copy only manifest files to cache npm install
COPY package.json package-lock.json ./
RUN npm install --package-lock-only
RUN npm ci

# Copy all source code and build
COPY . /app
RUN npm run build

# Stage 2: serve with nginx
FROM nginx:stable-alpine

# Remove default content
RUN rm -rf /usr/share/nginx/html/*

# Copy build artifacts
COPY --from=builder /app/build /usr/share/nginx/html

# Expose listening port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
