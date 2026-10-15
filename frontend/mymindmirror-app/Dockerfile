# Stage 1: Build the Vite React application
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./

# Use npm ci for clean, reproducible installations of all build dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the app (Vite compiles using devDependencies)
RUN npm run build

# Stage 2: Serve using Nginx with the custom proxy configuration
FROM nginx:alpine

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy your custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]